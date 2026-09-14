package com.theos.detectivehelper.desktop;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * 端口分配器。
 * <p>
 * 早期版本是从 8080 往上扫，问题是 8080/8081 这两个端口在本机分别被 nginx 和前端 dev server 占着，
 * 一启动就先撞两次；而且固定起点意味着多个实例之间还会互相抢。
 * 现在直接让操作系统给一个空闲的临时端口：{@code new ServerSocket(0)}。
 * 拿到后立刻关闭，把这个号码交给内嵌 Tomcat。
 * <p>
 * 这里有一个理论上存在的竞态（关闭到 Tomcat 真正 bind 之间，端口可能被别的进程抢走），
 * 但因为只绑定回环地址、且临时端口数量足够大，实际风险可以忽略；
 * 万一真撞上，{@link BackendServer} 会换端口重试。
 */
public final class PortAllocator {

    private static final String LOOPBACK = "127.0.0.1";

    private PortAllocator() {
    }

    /**
     * 申请一个当前空闲的、仅绑定回环地址的端口。
     */
    public static int allocateLoopbackPort() {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName(LOOPBACK))) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("无法分配本地端口: " + e.getMessage(), e);
        }
    }
}
