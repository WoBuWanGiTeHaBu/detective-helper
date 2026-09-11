package com.theos.detectivehelper.desktop;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 主窗口控制器
 */
public class MainWindow implements Initializable {

    @FXML
    private BorderPane mainPane;

    @FXML
    private VBox sidebar;

    @FXML
    private HBox topBar;

    @FXML
    private TabPane contentTabPane;

    @FXML
    private WebView webView;

    @FXML
    private Tab bookshelfTab;

    @FXML
    private Tab workspaceTab;

    @FXML
    private Tab relationGraphTab;

    @FXML
    private MenuBar menuBar;

    @FXML
    private Menu fileMenu;

    @FXML
    private Menu editMenu;

    @FXML
    private Menu viewMenu;

    @FXML
    private Menu helpMenu;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeUI();
        loadFrontend();
        setupEventHandlers();
    }

    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        // 设置WebView
        webView.getEngine().setJavaScriptEnabled(true);

        // 初始化菜单项
        initializeMenus();
    }

    /**
     * 加载前端页面
     */
    private void loadFrontend() {
        BackendServer server = DesktopApplication.getBackendServer();
        if (server != null && server.isRunning()) {
            int port = server.getPort();
            String frontendUrl = "http://localhost:" + port;

            // 加载前端页面
            webView.getEngine().load(frontendUrl);
        }
    }

    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 标签切换事件
        contentTabPane.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    handleTabChange(newValue);
                }
            }
        );
    }

    /**
     * 处理标签切换
     */
    private void handleTabChange(Tab newTab) {
        if (newTab == bookshelfTab) {
            loadFrontend();
        } else if (newTab == workspaceTab) {
            // 加载工作空间视图
            showWorkspaceView();
        } else if (newTab == relationGraphTab) {
            // 加载关系图视图
            showRelationGraphView();
        }
    }

    /**
     * 显示工作空间视图
     */
    private void showWorkspaceView() {
        // TODO: 实现工作空间视图
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("功能开发中");
        alert.setHeaderText(null);
        alert.setContentText("工作空间视图功能正在开发中");
        alert.showAndWait();
    }

    /**
     * 显示关系图视图
     */
    private void showRelationGraphView() {
        // TODO: 实现关系图视图
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("功能开发中");
        alert.setHeaderText(null);
        alert.setContentText("关系图视图功能正在开发中");
        alert.showAndWait();
    }

    /**
     * 初始化菜单
     */
    private void initializeMenus() {
        // 文件菜单
        MenuItem newItem = new MenuItem("新建案件书");
        MenuItem openItem = new MenuItem("打开案件书");
        MenuItem saveItem = new MenuItem("保存");
        MenuItem exitItem = new MenuItem("退出");

        newItem.setOnAction(event -> handleNewBook());
        openItem.setOnAction(event -> handleOpenBook());
        saveItem.setOnAction(event -> handleSave());
        exitItem.setOnAction(event -> handleExit());

        fileMenu.getItems().addAll(newItem, new SeparatorMenuItem(), openItem, saveItem, new SeparatorMenuItem(), exitItem);

        // 编辑菜单
        MenuItem undoItem = new MenuItem("撤销");
        MenuItem redoItem = new MenuItem("重做");
        MenuItem cutItem = new MenuItem("剪切");
        MenuItem copyItem = new MenuItem("复制");
        MenuItem pasteItem = new MenuItem("粘贴");

        undoItem.setOnAction(event -> handleUndo());
        redoItem.setOnAction(event -> handleRedo());
        cutItem.setOnAction(event -> handleCut());
        copyItem.setOnAction(event -> handleCopy());
        pasteItem.setOnAction(event -> handlePaste());

        editMenu.getItems().addAll(undoItem, redoItem, new SeparatorMenuItem(), cutItem, copyItem, pasteItem);

        // 视图菜单
        MenuItem refreshItem = new MenuItem("刷新");
        MenuItem fullscreenItem = new MenuItem("全屏");

        refreshItem.setOnAction(event -> handleRefresh());
        fullscreenItem.setOnAction(event -> handleFullscreen());

        viewMenu.getItems().addAll(refreshItem, fullscreenItem);

        // 帮助菜单
        MenuItem aboutItem = new MenuItem("关于");

        aboutItem.setOnAction(event -> handleAbout());

        helpMenu.getItems().add(aboutItem);
    }

    /**
     * 处理新建案件书
     */
    private void handleNewBook() {
        // TODO: 实现新建案件书功能
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("新建案件书");
        alert.setHeaderText(null);
        alert.setContentText("新建案件书功能正在开发中");
        alert.showAndWait();
    }

    /**
     * 处理打开案件书
     */
    private void handleOpenBook() {
        // TODO: 实现打开案件书功能
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("打开案件书");
        alert.setHeaderText(null);
        alert.setContentText("打开案件书功能正在开发中");
        alert.showAndWait();
    }

    /**
     * 处理保存
     */
    private void handleSave() {
        // TODO: 实现保存功能
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("保存");
        alert.setHeaderText(null);
        alert.setContentText("保存功能正在开发中");
        alert.showAndWait();
    }

    /**
     * 处理退出
     */
    private void handleExit() {
        System.exit(0);
    }

    /**
     * 处理撤销
     */
    private void handleUndo() {
        // TODO: 实现撤销功能
    }

    /**
     * 处理重做
     */
    private void handleRedo() {
        // TODO: 实现重做功能
    }

    /**
     * 处理剪切
     */
    private void handleCut() {
        // TODO: 实现剪切功能
    }

    /**
     * 处理复制
     */
    private void handleCopy() {
        // TODO: 实现复制功能
    }

    /**
     * 处理粘贴
     */
    private void handlePaste() {
        // TODO: 实现粘贴功能
    }

    /**
     * 处理刷新
     */
    private void handleRefresh() {
        webView.getEngine().reload();
    }

    /**
     * 处理全屏
     */
    private void handleFullscreen() {
        Stage stage = DesktopApplication.getPrimaryStage();
        stage.setFullScreen(!stage.isFullScreen());
    }

    /**
     * 处理关于
     */
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于侦探助手");
        alert.setHeaderText("侦探助手 v1.0.0");
        alert.setContentText("一个帮助侦探整理案件信息的桌面应用");
        alert.showAndWait();
    }

}