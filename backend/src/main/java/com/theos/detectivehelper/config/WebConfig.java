package com.theos.detectivehelper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web配置 - 处理CORS、静态资源等
 * <p>
 * 前端开发期走 vite proxy（/api → localhost:8081），同源，不触发跨域；
 * 这里的配置是为了「直连后端」和「Electron 壳」这类场景兜底。
 * <p>
 * 注意：allowCredentials(true) 不能与 allowedOrigins("*") 搭配使用，
 * 必须用 allowedOriginPatterns，否则运行时会抛
 * "When allowCredentials is true, allowedOrigins cannot contain the special value \"*\""。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 封面等静态文件映射：GET /files/covers/{文件名} → data/covers/{文件名}。
     * 前端 coverUrl() 以 /files 为前缀拼接 coverValue（covers/book_x.webp）。
     * 只开放 covers 子目录，不暴露整个 data 目录（里面有 SQLite 库文件）。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String coversLocation = "file:" + Paths.get("data", "covers").toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/files/covers/**")
                .addResourceLocations(coversLocation);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 开发期直连常用来源；生产若同源部署可收紧为具体域名
                .allowedOriginPatterns(
                        "http://localhost:[*]",
                        "http://127.0.0.1:[*]"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

}
