package com.theos.detectivehelper.config;

import com.theos.detectivehelper.repository.SchemaMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * SQLite 相关的启动期准备：目录 + 老库补列。
 * <p>
 * <b>目录：</b>正常情况下 {@link DataDirEnvironmentPostProcessor} 已经建好了，
 * 这里再兜一次，覆盖「IDE 里单独跑 backend」「跑单元测试」这类不经过桌面壳的路径。
 * <p>
 * <b>补列：</b>{@code CREATE TABLE IF NOT EXISTS} 对已存在的表不会加新列，
 * SQLite 的 {@code ALTER TABLE} 又不支持 {@code IF NOT EXISTS}，只能在代码里查
 * {@code pragma_table_info} 后补。
 */
@Configuration
@DependsOnDatabaseInitialization
public class SqliteConfig {

    private static final Logger log = LoggerFactory.getLogger(SqliteConfig.class);

    /** 补列目标：book.content_updated_at（事件/页面/画布/关系图/族谱图变化才刷新） */
    private static final String MIGRATION_TABLE = "book";
    private static final String MIGRATION_COLUMN = "content_updated_at";
    private static final String MIGRATION_TYPE = "TEXT";

    private final SchemaMapper schemaMapper;
    private final StorageProperties storage;

    public SqliteConfig(SchemaMapper schemaMapper, StorageProperties storage) {
        this.schemaMapper = schemaMapper;
        this.storage = storage;
    }

    @PostConstruct
    public void ensureDirectories() {
        Path dataPath = storage.dataPath();
        try {
            Files.createDirectories(dataPath);
            for (Path dir : List.of(storage.coversDir(), storage.uploadsDir(),
                    storage.filesDir(), storage.logsDir())) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            throw new IllegalStateException("创建数据目录失败: " + dataPath, e);
        }
        log.info("数据目录就绪: {}", dataPath);
    }

    @PostConstruct
    public void migrateColumns() {
        try {
            if (schemaMapper.countColumn(MIGRATION_TABLE, MIGRATION_COLUMN) == 0) {
                schemaMapper.addColumn(MIGRATION_TABLE, MIGRATION_COLUMN, MIGRATION_TYPE);
                log.info("老库补列: {}.{} {}", MIGRATION_TABLE, MIGRATION_COLUMN, MIGRATION_TYPE);
            }
        } catch (Exception e) {
            // 全新库上 schema.sql 尚未执行时表还不存在，此时本来就不需要迁移，记一条警告即可
            log.warn("跳过列迁移（表可能尚未创建）: {}", e.getMessage());
        }
    }
}
