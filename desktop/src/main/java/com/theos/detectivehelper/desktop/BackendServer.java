package com.theos.detectivehelper.desktop;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 后端服务器管理类
 */
public class BackendServer implements ApplicationListener<ApplicationReadyEvent> {

    private static final int DEFAULT_PORT = 8080;
    private static final int MAX_PORT = 8999;

    private final PortAllocator portAllocator;
    private int port;
    private org.springframework.boot.SpringApplication application;
    private AtomicBoolean running = new AtomicBoolean(false);

    public BackendServer() {
        this.portAllocator = new PortAllocator(DEFAULT_PORT, MAX_PORT);
        this.port = portAllocator.allocatePort();
    }

    /**
     * 启动后端服务器
     */
    public void start() {
        if (running.get()) {
            return;
        }

        try {
            application = new SpringApplicationBuilder(com.theos.detectivehelper.DetectiveHelperApplication.class)
                    .properties("server.port=" + port)
                    .headless(true)
                    .build();

            application.addListeners(this);
            application.run();

        } catch (Exception e) {
            System.err.println("启动后端服务器失败: " + e.getMessage());
            // 尝试使用其他端口
            port = portAllocator.allocatePort();
            start();
        }
    }

    /**
     * 停止后端服务器
     */
    public void stop() {
        if (application != null) {
            application.stop();
            running.set(false);
        }
    }

    /**
     * 检查服务器是否运行
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 获取服务器端口
     */
    public int getPort() {
        return port;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        running.set(true);
        System.out.println("后端服务器已启动，监听端口: " + port);
    }

}