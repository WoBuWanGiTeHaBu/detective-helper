package com.theos.detectivehelper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置 - 处理CORS等
 * <p>
 * 前端开发期走 vite proxy（/api → localhost:8080），同源，不触发跨域；
 * 这里的配置是为了「直连后端」和「Electron 壳」这类场景兜底。
 * <p>
 * 注意：allowCredentials(true) 不能与 allowedOrigins("*") 搭配使用，
 * 必须用 allowedOriginPatterns，否则运行时会抛
 * "When allowCredentials is true, allowedOrigins cannot contain the special value \"*\""。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

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
