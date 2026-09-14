package com.theos.detectivehelper.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SQLite 相关配置。
 * 应用启动时确保数据目录和子目录存在，并为老库补齐 schema.sql 追加的列
 * （CREATE TABLE IF NOT EXISTS 对已存在的表不会加新列，SQLite 的 ALTER TABLE
 * 也不支持 IF NOT EXISTS，只能在代码里查 PRAGMA 后补）。
 */
@Configuration
public class SqliteConfig {

    private static final Path APP_DIR = Paths.get("data");

    private final JdbcTemplate jdbcTemplate;

    public SqliteConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    @PostConstruct
    public void migrateColumns() {
        addColumnIfMissing("book", "content_updated_at", "TEXT");
    }

    private void addColumnIfMissing(String table, String column, String type) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pragma_table_info('" + table + "') WHERE name = ?",
                Integer.class, column);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }

}
