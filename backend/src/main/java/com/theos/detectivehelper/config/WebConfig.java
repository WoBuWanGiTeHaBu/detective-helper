package com.theos.detectivehelper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web 配置：静态资源托管 + SPA 回退 + CORS 兜底。
 * <p>
 * 两类静态资源：
 * <ul>
 *   <li>{@code /files/covers/**} → 数据目录里的 {@code covers/}，只开放封面这一个子目录。
 *       不能把整个 data 目录暴露出去 —— 里面有 SQLite 库文件。</li>
 *   <li>{@code /**} → 前端构建产物（构建期被复制到 {@code classpath:/static/}）。
 *       前端用的是 {@code createWebHistory}（history 路由），所以
 *       {@code /workspace/12} 这种深层路由必须靠回退到 index.html 才能打开。</li>
 * </ul>
 * <p>
 * 注意：{@code spring.web.resources.add-mappings} 已在 application.yml 里关掉，
 * 否则 Spring Boot 自己也会注册一套 {@code /**} 规则，和这里的两套规则互相打架。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StorageProperties storage;

    public WebConfig(StorageProperties storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/covers/**")
                .addResourceLocations(storage.coversLocation());

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResourceResolver());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 开发期前端直连后端时用；打包后是同源，不会触发跨域
                .allowedOriginPatterns(
                        "http://localhost:[*]",
                        "http://127.0.0.1:[*]"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * SPA 回退解析器。
     * <p>
     * 规则（顺序很重要）：
     * <ol>
     *   <li>{@code api/**}、{@code files/**}：只找真实文件，找不到就返回 null，
     *       让它走 404。绝对不能回退成 index.html —— 否则前端调错接口时拿到的是
     *       一个 200 的 HTML 页面，解析 JSON 时才莫名其妙地报错。</li>
     *   <li>目录请求（{@code ""} 或结尾带斜杠）：直接给 index.html。</li>
     *   <li>能找到真实文件：给文件。</li>
     *   <li>看起来像静态资源（最后一段带扩展名，如 {@code /assets/index-abc.js}）：返回 null，
     *       让它老老实实 404，而不是返回一个内容类型是 text/html 的 index.html
     *       导致浏览器报 "Unexpected token <"。</li>
     *   <li>其余（形如 {@code /workspace/12} 的前端路由）：给 index.html。</li>
     * </ol>
     * 注意 {@code resourcePath} 是相对 location 的，<b>不带前导斜杠</b>，
     * 所以前缀比较用的是 {@code "api/"} 而不是 {@code "/api/"}。
     */
    static class SpaFallbackResourceResolver extends PathResourceResolver {

        private static final String[] BACKEND_PREFIXES = {"api/", "files/"};

        private static final String INDEX = "index.html";

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            if (isBackendPath(resourcePath)) {
                return super.getResource(resourcePath, location);
            }
            if (resourcePath.isEmpty() || resourcePath.endsWith("/")) {
                return super.getResource(INDEX, location);
            }

            Resource resource = super.getResource(resourcePath, location);
            if (resource != null) {
                return resource;
            }
            if (looksLikeAsset(resourcePath)) {
                return null;
            }
            return super.getResource(INDEX, location);
        }

        private static boolean isBackendPath(String path) {
            for (String prefix : BACKEND_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean looksLikeAsset(String path) {
            int slash = path.lastIndexOf('/');
            String lastSegment = slash >= 0 ? path.substring(slash + 1) : path;
            return lastSegment.indexOf('.') >= 0;
        }
    }
}
