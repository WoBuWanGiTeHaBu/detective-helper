package com.theos.detectivehelper.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 桌面应用入口。
 * <p>
 * 启动顺序（顺序很关键，不能调换）：
 * <ol>
 *   <li>解析应用目录与数据目录，把数据目录写成系统属性 —— 必须早于任何 Spring 代码，
 *       否则数据源会拿着相对路径去找库；</li>
 *   <li>安装崩溃记录器 —— 必须早于 JavaFX 初始化，否则启动期异常会静默消失；</li>
 *   <li>装载 FXML、显示窗口（此时盖着启动遮罩）；</li>
 *   <li>由 {@link MainWindow} 在后台线程拉起内嵌后端，就绪后再把 WebView 指过去。</li>
 * </ol>
 */
public class DesktopApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(DesktopApplication.class);

    private static final String WINDOW_TITLE = "Detective Helper · 推演录";

    private static final double INITIAL_WIDTH = 1280;
    private static final double INITIAL_HEIGHT = 840;
    private static final double MIN_WIDTH = 1000;
    private static final double MIN_HEIGHT = 680;

    private MainWindow controller;

    @Override
    public void start(Stage stage) throws IOException {
        Path appDir = AppPaths.resolveAppDir();
        Path dataDir = AppPaths.resolveDataDir(appDir);

        // 唯一的真相来源：先落系统属性，后端的 EnvironmentPostProcessor 会读它
        System.setProperty(AppPaths.DATA_DIR_PROPERTY, dataDir.toAbsolutePath().toString());

        Path crashLog = CrashReporter.install(AppPaths.logsDir(dataDir));

        log.info("应用目录: {}", appDir);
        log.info("数据目录: {}", dataDir);
        log.info("环境: {}", AppPaths.describeOs());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-window.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        controller.init(stage, dataDir, crashLog);

        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle(WINDOW_TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        try (InputStream iconStream = getClass().getResourceAsStream("/icon.png")) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        }

        stage.show();

        // 窗口先出来，后端后台起来 —— 不要反过来，否则用户会盯着空白屏幕十几秒
        controller.bootBackendAsync();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    /** 数据目录下是否已经有库文件（用来判断是不是首次启动） */
    static boolean isFirstRun(Path dataDir) {
        return !Files.exists(dataDir.resolve("detective-helper.db"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
