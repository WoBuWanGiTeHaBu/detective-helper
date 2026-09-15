package com.theos.detectivehelper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 数据目录配置。
 * <p>
 * <b>业务代码取路径的唯一入口</b> —— 任何地方都不允许再出现
 * {@code Paths.get("data", "covers")} 这类硬编码相对路径。
 * 相对路径在打包后的表现取决于「当前工作目录」，而双击 exe 时那是完全不可控的
 * （可能是 C:\Windows\System32，也可能是用户上次打开文件的位置），
 * 会导致数据散落在多个地方且用户找不到。这个项目已经踩过一次：
 * 仓库里同时躺着 {@code ./data} 和 {@code ./backend/data} 两份 SQLite 库。
 * <p>
 * 值的来源链条：
 * 桌面壳 AppPaths → 系统属性 {@code detectivehelper.data.dir}
 * → {@link DataDirEnvironmentPostProcessor} 落成绝对路径 → yml 里
 * {@code detective-helper.storage.data-dir=${detectivehelper.data.dir:./data}}。
 * 默认值 {@code ./data} 只服务于「IDE 里单独跑 backend」这种开发场景。
 */
@ConfigurationProperties(prefix = "detective-helper.storage")
public class StorageProperties {

    /** data 目录（绝对路径）。子目录名与 schema/前端约定保持一致，不要单独改 */
    private String dataDir = "./data";

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public Path dataPath() {
        return Paths.get(dataDir).toAbsolutePath().normalize();
    }

    public Path coversDir() {
        return dataPath().resolve("covers");
    }

    public Path uploadsDir() {
        return dataPath().resolve("uploads");
    }

    public Path filesDir() {
        return dataPath().resolve("files");
    }

    public Path logsDir() {
        return dataPath().resolve("logs");
    }

    /**
     * 给 Spring 静态资源用的 location 字符串。
     * <p>
     * {@code addResourceLocations} 要求目录 location 以 {@code /} 结尾，
     * 否则最后一节会被当成文件名前缀，拼出 {@code .../coversbook_1.webp} 这种路径。
     * {@code Path.toUri()} 只在目录真实存在时才补斜杠，所以这里显式保证。
     */
    public String coversLocation() {
        String uri = coversDir().toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }
}
