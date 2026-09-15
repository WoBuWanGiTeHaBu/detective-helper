package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.config.StorageProperties;
import com.theos.detectivehelper.repository.SystemMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统自检接口。
 * <p>
 * 桌面壳在把 WebView 指过去之前会轮询这里 —— 「Spring 上下文 active」不等于「服务能应答」，
 * 所以要做一次真实的 HTTP 往返 + 一次真实的数据库查询才算就绪。
 * 冒烟测试的第一阶段也复用这个接口做健康检查。
 * <p>
 * 这是新增的接口（原 OpenAPI 文档里没有），属于纯追加，不影响既有契约。
 */
@RestController
@RequestMapping("/api")
public class SystemController {

    private final SystemMapper systemMapper;
    private final StorageProperties storage;
    private final String appName;
    private final String appVersion;

    public SystemController(SystemMapper systemMapper,
                            StorageProperties storage,
                            @Value("${detective-helper.app.name:Detective Helper}") String appName,
                            @Value("${detective-helper.app.version:dev}") String appVersion) {
        this.systemMapper = systemMapper;
        this.storage = storage;
        this.appName = appName;
        this.appVersion = appVersion;
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("app", appName);
        data.put("version", appVersion);
        data.put("dataDir", storage.dataPath().toString());
        data.put("database", systemMapper.ping() != null ? "UP" : "DOWN");
        return Result.success(data);
    }
}
