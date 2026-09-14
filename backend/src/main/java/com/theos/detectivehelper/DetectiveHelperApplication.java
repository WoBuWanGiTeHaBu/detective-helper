package com.theos.detectivehelper;

import com.theos.detectivehelper.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 侦探助手后端应用主类。
 * <p>
 * 两种启动方式共用这一个入口：
 * <ul>
 *   <li>开发期：{@code mvn -pl backend spring-boot:run}，端口取 application.yml 的 8081；</li>
 *   <li>打包后：桌面壳用 {@code SpringApplicationBuilder.run("--server.port=<空闲端口>")} 启动，
 *       命令行参数优先级高于 yml，所以会覆盖掉 8081。</li>
 * </ul>
 */
@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class DetectiveHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(DetectiveHelperApplication.class, args);
    }

}
