package com.nushungry.config;

import com.nushungry.interceptor.RoleCheckInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RoleCheckInterceptor roleCheckInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 禁用 WebMvcConfigurer 的 CORS 配置，使用 CorsConfig 的统一配置
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源访问路径 - 图片
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations("file:uploads/images/");

        // 配置静态资源访问路径 - 缩略图
        registry.addResourceHandler("/uploads/thumbnails/**")
                .addResourceLocations("file:uploads/thumbnails/");

        // 兼容旧的路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册角色检查拦截器
        registry.addInterceptor(roleCheckInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                    "/api/admin/auth/login",  // 管理员登录接口
                    "/api/admin/auth/refresh", // 管理员token刷新接口
                    "/api/auth/**"             // 普通用户认证接口
                );
    }
}
