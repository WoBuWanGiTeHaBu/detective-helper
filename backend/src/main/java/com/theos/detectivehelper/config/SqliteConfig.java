package com.theos.detectivehelper.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SQLite 相关配置。
 * 应用启动时确保数据目录和子目录存在。
 */
@Configuration
public class SqliteConfig {

    private static final Path APP_DIR = Paths.get("data");

    @PostConstruct
    public void initDirectories() {
        Path coversDir = APP_DIR.resolve("covers");
        Path filesDir = APP_DIR.resolve("files");
        Path uploadsDir = APP_DIR.resolve("uploads");

        try {
            Files.createDirectories(APP_DIR);
            Files.createDirectories(coversDir);
            Files.createDirectories(filesDir);
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            throw new RuntimeException("创建数据目录失败: " + APP_DIR.toAbsolutePath(), e);
        }
    }
}