package com.theos.detectivehelper.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 在数据源被创建之前，把数据目录准备好并把绝对路径写回 Environment。
 * <p>
 * <b>为什么必须做成 EnvironmentPostProcessor：</b>
 * SQLite 只会创建 {@code .db} 文件，<b>不会创建父目录</b>。如果 data 目录不存在，
 * HikariCP 初始化时会直接报 {@code path to 'xxx.db': ... does not exist}。
 * 而 {@code @PostConstruct}（比如以前 SqliteConfig 里的写法）执行得太晚 ——
 * 那时数据源早就创建完了。EnvironmentPostProcessor 在
 * {@code ApplicationEnvironmentPreparedEvent} 阶段执行，早于所有自动装配。
 * <p>
 * 注册位置：{@code src/main/resources/META-INF/spring.factories}
 * （EnvironmentPostProcessor 至今仍然走 spring.factories，不是 AutoConfiguration.imports）。
 */
public class DataDirEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** 桌面壳与本类之间的契约键 */
    public static final String DATA_DIR_PROPERTY = "detectivehelper.data.dir";

    private static final String STORAGE_DATA_DIR_PROPERTY = "detective-helper.storage.data-dir";

    private static final String PROPERTY_SOURCE_NAME = "detectiveHelperDataDir";

    private static final String DEFAULT_DATA_DIR = "./data";

    /** 与 AppPaths.SUB_DIRECTORIES 保持一致 */
    private static final String[] SUB_DIRECTORIES = {"covers", "uploads", "files", "logs", "backups"};

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String configured = environment.getProperty(DATA_DIR_PROPERTY);

        Path dataDir = (configured == null || configured.isBlank())
                ? Paths.get(DEFAULT_DATA_DIR)
                : Paths.get(configured);
        dataDir = dataDir.toAbsolutePath().normalize();

        try {
            Files.createDirectories(dataDir);
            for (String sub : SUB_DIRECTORIES) {
                Files.createDirectories(dataDir.resolve(sub));
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "无法创建数据目录 " + dataDir + "：" + e.getMessage()
                            + "（可用 -D" + DATA_DIR_PROPERTY + "=<可写目录> 指定其他位置）", e);
        }

        Map<String, Object> resolved = new LinkedHashMap<>();
        resolved.put(DATA_DIR_PROPERTY, dataDir.toString());
        resolved.put(STORAGE_DATA_DIR_PROPERTY, dataDir.toString());

        // addFirst：让解析后的绝对路径压过 yml 里的默认值
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, resolved));
    }

    @Override
    public int getOrder() {
        // 尽量靠前，但不必抢到最前 —— 数据源相关的自动装配远在其后
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
