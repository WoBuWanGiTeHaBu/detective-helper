package com.theos.detectivehelper.desktop;

import com.theos.detectivehelper.DetectiveHelperApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 内嵌后端服务的生命周期管理。
 * <p>
 * 只做一件事：在 {@code 127.0.0.1} 的随机空闲端口上把 Spring Boot 跑起来，
 * 并且提供一个「真的能应答」的就绪判断。
 * <p>
 * <b>端口为什么用命令行参数传：</b>Spring Boot 的属性优先级里，命令行参数高于
 * {@code application.yml}。如果改用 {@code setDefaultProperties}，yml 里那句
 * {@code server.port: 8081}（开发期给 vite proxy 用的）会把它盖掉，打包后就会去抢 8081。
 */
public final class BackendServer {

    private static final Logger log = LoggerFactory.getLogger(BackendServer.class);

    /** 首次启动要初始化数据库、扫描 classpath，冷启动十几秒很正常，给足余量 */
    private static final Duration READY_TIMEOUT = Duration.ofSeconds(120);

    private static final Duration PROBE_INTERVAL = Duration.ofMillis(200);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private static final String BIND_ADDRESS = "127.0.0.1";

    private final int port;

    private ConfigurableApplicationContext context;

    public BackendServer() {
        this.port = PortAllocator.allocateLoopbackPort();
    }

    public int getPort() {
        return port;
    }

    public String baseUrl() {
        return "http://" + BIND_ADDRESS + ":" + port;
    }

    /**
     * 启动 Spring Boot。<b>会阻塞十几秒，必须在后台线程调用。</b>
     */
    public void start() {
        context = new SpringApplicationBuilder(DetectiveHelperApplication.class)
                .headless(true)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=" + port,
                        "--server.address=" + BIND_ADDRESS
                );
    }

    /**
     * 等后端真的能应答。
     * <p>
     * Spring 的 {@code run()} 返回 ≠ 端口可服务，而且 Tomcat bind 端口和第一次 HTTP 握手之间
     * 还有一小段空窗，所以这里必须做真实的 HTTP 探测，不能只看上下文是否 active。
     */
    public boolean awaitHttpReady(Duration timeout) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/health"))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        long deadline = System.nanoTime() + timeout.toNanos();
        int attempt = 0;
        while (System.nanoTime() < deadline) {
            attempt++;
            try {
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) {
                    log.info("后端服务已就绪: {} （第 {} 次探测）", baseUrl(), attempt);
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // 端口还没起来的时候 connect 会被拒，属于预期内，继续轮询
            }
            try {
                Thread.sleep(PROBE_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("后端服务在 {} 秒内没有应答 /api/health", timeout.getSeconds());
        return false;
    }

    public void stop() {
        ConfigurableApplicationContext current = context;
        if (current == null) {
            return;
        }
        try {
            current.close();
            log.info("后端服务已停止");
        } catch (Exception e) {
            log.warn("停止后端服务时出现异常: {}", e.getMessage());
        } finally {
            context = null;
        }
    }

    public boolean isRunning() {
        ConfigurableApplicationContext current = context;
        return current != null && current.isActive();
    }
}
