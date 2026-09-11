package com.theos.detectivehelper.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 桌面应用主类
 */
public class DesktopApplication extends Application {

    private static Stage primaryStage;
    private static BackendServer backendServer;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.setTitle("侦探助手 - Detective Helper");

        // 启动后端服务器
        startBackendServer();

        // 加载主界面
        loadMainWindow();

        // 设置应用关闭行为
        primaryStage.setOnCloseRequest(event -> {
            shutdown();
            System.exit(0);
        });

        primaryStage.show();
    }

    /**
     * 启动后端服务器
     */
    private void startBackendServer() {
        try {
            backendServer = new BackendServer();
            backendServer.start();
            System.out.println("后端服务器已启动");
        } catch (Exception e) {
            System.err.println("启动后端服务器失败: " + e.getMessage());
        }
    }

    /**
     * 加载主窗口
     */
    private void loadMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-window.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

        } catch (Exception e) {
            System.err.println("加载主窗口失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 应用程序关闭
     */
    private void shutdown() {
        if (backendServer != null) {
            backendServer.stop();
        }
    }

    /**
     * 获取主舞台
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * 获取后端服务器
     */
    public static BackendServer getBackendServer() {
        return backendServer;
    }

    public static void main(String[] args) {
        launch(args);
    }

}