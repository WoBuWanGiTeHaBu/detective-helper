package com.theos.detectivehelper.desktop;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 端口分配器
 */
public class PortAllocator {

    private final AtomicInteger currentPort;
    private final int maxPort;
    private final Set<Integer> usedPorts = new HashSet<>();

    public PortAllocator(int startPort, int maxPort) {
        this.currentPort = new AtomicInteger(startPort);
        this.maxPort = maxPort;
    }

    /**
     * 分配可用端口
     */
    public int allocatePort() {
        int attempts = 0;
        int maxAttempts = 100;

        while (attempts < maxAttempts) {
            int port = currentPort.getAndIncrement();

            // 如果超过最大端口，重置
            if (port > maxPort) {
                currentPort.set(8000);
                port = currentPort.getAndIncrement();
            }

            if (!usedPorts.contains(port) && isPortAvailable(port)) {
                usedPorts.add(port);
                return port;
            }

            attempts++;
        }

        throw new RuntimeException("无法分配可用端口");
    }

    /**
     * 释放端口
     */
    public void releasePort(int port) {
        usedPorts.remove(port);
    }

    /**
     * 检查端口是否可用
     */
    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 获取已使用的端口数量
     */
    public int getUsedPortCount() {
        return usedPorts.size();
    }

}