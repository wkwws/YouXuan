package com.example.secondhand_project.conf;

import com.example.secondhand_project.admin.interceptor.AdminJwtInterceptor;
import com.example.secondhand_project.interceptor.interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConf implements WebMvcConfigurer {

    @Autowired
    private interceptor interceptor;

    @Autowired
    private AdminJwtInterceptor adminJwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 管理端接口走独立鉴权，绝不能落到 C 端拦截器
        registry.addInterceptor(adminJwtInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/login")
                .order(1);

        registry.addInterceptor(interceptor)
                .excludePathPatterns(
                        "/auth/**",
                        "/categories",
                        "/admin/**",
                        // Swagger UI 静态资源（不拦截）
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html")
                .order(2);
    }
}
