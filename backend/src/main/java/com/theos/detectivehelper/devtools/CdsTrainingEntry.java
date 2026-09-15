package com.theos.detectivehelper.devtools;

import com.theos.detectivehelper.DetectiveHelperApplication;
import org.springframework.boot.SpringApplication;

/**
 * AppCDS 训练入口 —— 只在构建期用来生成 share archive，运行期永远不会被调用。
 *
 * <p><b>为什么需要它。</b> {@code -XX:ArchiveClassesAtExit} 只在 JVM <em>正常退出</em>时才写归档。
 * 桌面外壳停后端用的是 {@code child.kill()}，在 Windows 上等于 TerminateProcess，
 * JVM 没有任何机会落盘 —— 于是"直接给线上进程开 CDS"的后果是每一次启动都白付一遍
 * 生成归档的代价（实测启动从 7.5 秒涨到 48 秒）。所以训练必须是"启动一次、自己退出"
 * 的独立进程，而这个"自己退出"必须写在 Java 代码里。
 *
 * <p><b>为什么必须放在 {@code src/main/java}（也就是必须留在应用自己的 jar 里）。</b>
 * CDS 会校验运行期的 classpath 与训练时是否一致，而动态归档又不允许 classpath 上出现
 * 非空目录。把训练入口挂在旁边一个单独的目录里，classpath 立刻多出一个目录，
 * 归档就会被 JVM 拒绝。放进同一个 jar 是唯一自洽的做法 —— 代价是这个类会随包发布，
 * 但它不被任何东西引用，也没有副作用。
 *
 * <p>调用方：{@code scripts/make-cds-archive.ps1}
 *
 * @see com.theos.detectivehelper.DetectiveHelperApplication
 */
public final class CdsTrainingEntry {

    private CdsTrainingEntry() {
    }

    public static void main(String[] args) {
        // 用真实的 Spring 上下文完整启动一遍：只有这样，启动期真正被加载的类
        // 才会被记进归档。换成"只加载几个类"的假训练基本没有收益。
        SpringApplication.run(DetectiveHelperApplication.class, args);

        // 这行标记是给构建脚本确认"确实启动成功了"用的，别随便改。
        System.out.println("CDS_TRAIN_READY");
        System.out.flush();

        // 正常退出，JVM 会在 exit path 里把归档写出来
        System.exit(0);
    }
}
