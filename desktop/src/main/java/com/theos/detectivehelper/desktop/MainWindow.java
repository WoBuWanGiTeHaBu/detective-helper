package com.theos.detectivehelper.desktop;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

/**
 * 主窗口控制器。
 * <p>
 * 外壳的职责边界：起本地服务、承载前端页面、提供少数「网页里做不到」的原生能力
 * （打开数据目录 / 打开日志 / 缩放 / 全屏 / 关于）。
 * 页面内的导航一律交给前端自己的路由，外壳不重复实现一遍。
 */
public class MainWindow {

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private static final Duration READY_TIMEOUT = Duration.ofSeconds(120);

    /** 端口绑定偶发失败时的重试次数（每次换一个新端口） */
    private static final int START_ATTEMPTS = 3;

    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 2.0;
    private static final double ZOOM_STEP = 0.1;
    private static final double ZOOM_DEFAULT = 1.0;

    private static final String SHELF_ROUTE = "/bookshelf";

    // ---------- FXML 注入 ----------

    @FXML private BorderPane mainPane;
    @FXML private MenuBar menuBar;

    @FXML private Menu fileMenu;
    @FXML private Menu editMenu;
    @FXML private Menu viewMenu;
    @FXML private Menu helpMenu;

    @FXML private MenuItem openDataDirItem;
    @FXML private MenuItem openLogItem;
    @FXML private MenuItem exitItem;
    @FXML private MenuItem undoItem;
    @FXML private MenuItem redoItem;
    @FXML private MenuItem cutItem;
    @FXML private MenuItem copyItem;
    @FXML private MenuItem pasteItem;
    @FXML private MenuItem refreshItem;
    @FXML private MenuItem zoomInItem;
    @FXML private MenuItem zoomOutItem;
    @FXML private MenuItem zoomResetItem;
    @FXML private MenuItem fullscreenItem;
    @FXML private MenuItem helpItem;
    @FXML private MenuItem aboutItem;

    @FXML private VBox sidebar;
    @FXML private Label versionLabel;
    @FXML private Button shelfButton;
    @FXML private Button dataDirButton;
    @FXML private Button refreshButton;
    @FXML private Button aboutButton;

    @FXML private StackPane contentStack;
    @FXML private TabPane contentTabPane;
    @FXML private Tab bookshelfTab;
    @FXML private WebView webView;

    @FXML private VBox loadingOverlay;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label loadingLabel;
    @FXML private Label loadingHint;
    @FXML private Button retryButton;

    @FXML private HBox topBar;
    @FXML private Label statusLabel;
    @FXML private Label dataDirLabel;

    // ---------- 运行期状态 ----------

    private Stage stage;
    private Path dataDir;
    private Path crashLog;
    private BackendServer server;
    private Thread bootThread;
    private boolean shuttingDown;

    @FXML
    public void initialize() {
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().setUserAgent(webView.getEngine().getUserAgent() + " DetectiveHelper/Desktop");

        // WebView 报错时不要留个白屏在那，给一条能看懂的信息
        webView.getEngine().getLoadWorker().exceptionProperty().addListener((obs, old, error) -> {
            if (error != null) {
                log.warn("页面加载失败: {}", error.getMessage());
                setStatus("页面加载失败：" + error.getMessage());
            }
        });

        // 把加载成功也记一笔。打包后没有控制台，"白屏"是最难定位的故障 ——
        // 有这条日志就能一眼分清是「页面压根没加载」还是「页面加载了、前端自己报错」。
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                log.info("前端页面已渲染: {}", webView.getEngine().getLocation());
            } else if (state == Worker.State.FAILED) {
                log.warn("前端页面渲染失败: {}", webView.getEngine().getLocation());
            }
        });

        setupAccelerators();
    }

    /**
     * 由 {@link DesktopApplication} 在窗口显示前注入运行期上下文。
     */
    void init(Stage stage, Path dataDir, Path crashLog) {
        this.stage = stage;
        this.dataDir = dataDir;
        this.crashLog = crashLog;

        versionLabel.setText("v" + resolveVersion());
        dataDirLabel.setText(dataDir.toString());
        dataDirLabel.setTooltip(new Tooltip("数据目录：" + dataDir));

        setStatus("准备中…");
        showOverlay("正在启动本地服务…",
                DesktopApplication.isFirstRun(dataDir)
                        ? "首次启动需要初始化数据库，通常需要十几秒。"
                        : "正在初始化运行环境，请稍候。",
                false);

        stage.setOnCloseRequest(event -> {
            shutdown();
            Platform.exit();
        });
    }

    // ---------- 启动流程 ----------

    /**
     * 在后台线程拉起内嵌后端，就绪后把 WebView 指过去。
     * <p>
     * 端口绑定偶发失败（被别的进程抢走）时换端口重试，最多 {@value #START_ATTEMPTS} 次。
     */
    void bootBackendAsync() {
        if (bootThread != null && bootThread.isAlive()) {
            return;
        }

        bootThread = new Thread(() -> {
            Throwable lastError = null;

            for (int attempt = 1; attempt <= START_ATTEMPTS; attempt++) {
                BackendServer candidate = new BackendServer();
                try {
                    log.info("第 {}/{} 次尝试启动后端，端口 {}", attempt, START_ATTEMPTS, candidate.getPort());
                    candidate.start();
                    server = candidate;

                    if (!candidate.awaitHttpReady(READY_TIMEOUT)) {
                        throw new IllegalStateException(
                                "本地服务在 " + READY_TIMEOUT.getSeconds() + " 秒内没有应答 /api/health");
                    }

                    Platform.runLater(() -> onBackendReady(candidate));
                    return;

                } catch (Throwable t) {
                    lastError = t;
                    log.error("第 {} 次启动后端失败", attempt, t);
                    candidate.stop();
                    server = null;
                }
            }

            Throwable failure = lastError;
            CrashReporter.record("后端启动失败", failure, "数据目录 " + dataDir);
            String message = failure == null || failure.getMessage() == null
                    ? "未知错误"
                    : failure.getMessage();
            Platform.runLater(() -> onBackendFailed(message));
        }, "backend-starter");

        bootThread.setDaemon(true);
        bootThread.start();
    }

    private void onBackendReady(BackendServer ready) {
        hideOverlay();
        setStatus("本地服务已就绪 · 端口 " + ready.getPort());
        String url = ready.baseUrl() + SHELF_ROUTE;
        log.info("加载前端页面: {}", url);
        webView.getEngine().load(url);
        contentTabPane.getSelectionModel().select(bookshelfTab);
    }

    private void onBackendFailed(String message) {
        String hint = "本地服务启动失败：" + message
                + "\n\n可以试试：查看日志（文件菜单 → 打开日志文件）确认原因，然后点下面的「重试」。"
                + "\n数据目录：" + dataDir;
        showOverlay("本地服务启动失败", hint, true);
        setStatus("本地服务启动失败");
        log.error("后端启动失败，已展示重试入口");
    }

    // ---------- 文件菜单 ----------

    @FXML
    private void onOpenDataDir() {
        openInShell(dataDir);
    }

    @FXML
    private void onOpenLog() {
        Path logDir = AppPaths.logsDir(dataDir);
        Path target = crashLog != null && java.nio.file.Files.exists(crashLog) ? crashLog : logDir;
        openInShell(target);
    }

    @FXML
    private void onExit() {
        shutdown();
        Platform.exit();
    }

    // ---------- 编辑菜单：转发给页面 ----------

    @FXML
    private void onUndo() {
        execInPage("document.execCommand('undo')");
    }

    @FXML
    private void onRedo() {
        execInPage("document.execCommand('redo')");
    }

    @FXML
    private void onCut() {
        execInPage("document.execCommand('cut')");
    }

    @FXML
    private void onCopy() {
        execInPage("document.execCommand('copy')");
    }

    @FXML
    private void onPaste() {
        execInPage("document.execCommand('paste')");
    }

    // ---------- 视图菜单 ----------

    @FXML
    private void onRefresh() {
        WebEngine engine = webView.getEngine();
        if (engine.getLocation() == null || engine.getLocation().isBlank()) {
            // 后端还没就绪时点刷新，就再踢一次启动流程
            bootBackendAsync();
            return;
        }
        engine.reload();
    }

    @FXML
    private void onZoomIn() {
        applyZoom(webView.getZoom() + ZOOM_STEP);
    }

    @FXML
    private void onZoomOut() {
        applyZoom(webView.getZoom() - ZOOM_STEP);
    }

    @FXML
    private void onZoomReset() {
        applyZoom(ZOOM_DEFAULT);
    }

    @FXML
    private void onFullscreen() {
        if (stage != null) {
            stage.setFullScreen(!stage.isFullScreen());
        }
    }

    @FXML
    private void onShelf() {
        if (server == null || !server.isRunning()) {
            bootBackendAsync();
            return;
        }
        webView.getEngine().load(server.baseUrl() + SHELF_ROUTE);
    }

    // ---------- 帮助菜单 ----------

    @FXML
    private void onHelp() {
        String text = """
                推演录是一本单机的案件推演笔记：左栏管理案件，工作区里记事件、页面，
                在页面的画布上摆对象、连线、写注解、排时间线，另外还有人物关系图和族谱图。

                几个常用操作
                · 案件书架：新建 / 重命名 / 换封面 / 长按拖动排序
                · 工作区：进入案件后建事件与页面，双击页面进入画布
                · 画布：「移动画布」只挪视野，「移动内容」才会真的挪动物件坐标
                · 关系图：点节点切换中心，只显示与中心相关的连线

                数据都在本地
                · 数据目录：文件菜单 → 打开数据目录（备份就是把这个目录拷走）
                · 出问题时：文件菜单 → 打开日志文件

                快捷操作
                · Ctrl+R 刷新    Ctrl+= / Ctrl+- 缩放    Ctrl+0 实际大小    F11 全屏
                """;
        showInfo("使用说明", "推演录 · 使用说明", text);
    }

    @FXML
    private void onAbout() {
        String text = "版本：v" + resolveVersion()
                + "\n数据目录：" + dataDir
                + "\n运行环境：" + AppPaths.describeOs()
                + "\n\n一个帮助推理游戏玩家整理案件信息的单机笔记工具。";
        showInfo("关于 Detective Helper", "Detective Helper · 推演录", text);
    }

    // ---------- 遮罩 / 重试 ----------

    @FXML
    private void onRetry() {
        showOverlay("正在重试…", "正在重新启动本地服务，请稍候。", false);
        bootBackendAsync();
    }

    private void showOverlay(String title, String hint, boolean withRetry) {
        loadingLabel.setText(title);
        loadingHint.setText(hint);
        loadingSpinner.setVisible(!withRetry);
        loadingOverlay.setVisible(true);
        retryButton.setVisible(withRetry);
        retryButton.setManaged(withRetry);
    }

    private void hideOverlay() {
        loadingOverlay.setVisible(false);
    }

    // ---------- 收尾 ----------

    void shutdown() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    // ---------- 内部工具 ----------

    private void setupAccelerators() {
        refreshItem.setAccelerator(KeyCombination.keyCombination("Shortcut+R"));
        zoomInItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Plus"));
        zoomOutItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Minus"));
        zoomResetItem.setAccelerator(KeyCombination.keyCombination("Shortcut+0"));
        fullscreenItem.setAccelerator(KeyCombination.keyCombination("F11"));
        openDataDirItem.setAccelerator(KeyCombination.keyCombination("Shortcut+D"));
        aboutItem.setAccelerator(KeyCombination.keyCombination("F1"));
    }

    private void applyZoom(double zoom) {
        double clamped = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, Math.round(zoom * 100) / 100.0));
        webView.setZoom(clamped);
        setStatus("缩放 " + Math.round(clamped * 100) + "%");
    }

    private void execInPage(String javascript) {
        WebEngine engine = webView.getEngine();
        if (engine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            try {
                engine.executeScript(javascript);
            } catch (Exception e) {
                log.debug("页面内执行脚本失败: {}", e.getMessage());
            }
        }
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void showInfo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        if (stage != null && stage.getScene() != null) {
            alert.initOwner(stage);
        }
        alert.showAndWait();
    }

    /**
     * 用系统默认程序打开一个目录或文件。
     * <p>
     * 刻意不用 {@code java.awt.Desktop}：在一个纯 JavaFX 进程里初始化 AWT 工具箱，
     * 在某些 Windows 环境下会和 JavaFX 的事件循环打架、表现为启动卡死。
     * 直接调平台自带的 opener 更省事也更安全。
     */
    private void openInShell(Path path) {
        if (path == null) {
            return;
        }
        String target = path.toAbsolutePath().toString();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", target).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", target).start();
            } else {
                new ProcessBuilder("xdg-open", target).start();
            }
        } catch (IOException e) {
            log.warn("打开 {} 失败: {}", target, e.getMessage());
            setStatus("打开失败：" + e.getMessage());
        }
    }

    private String resolveVersion() {
        String fromManifest = DesktopApplication.class.getPackage() == null
                ? null
                : DesktopApplication.class.getPackage().getImplementationVersion();
        return fromManifest != null && !fromManifest.isBlank() ? fromManifest : "1.0.0";
    }
}
