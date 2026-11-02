package com.example.secondhand_project.conf;

import com.example.secondhand_project.interceptor.interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConf implements WebMvcConfigurer {

    @Autowired
    private interceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
     registry.addInterceptor(interceptor).excludePathPatterns( "/auth/**",
             "/categories",
             // Swagger UI 静态资源（不拦截）
             "/swagger-ui/**",
             "/v3/api-docs/**",
             "/swagger-ui.html");
     // 商品详情的公开访问由 interceptor 内部的 GET 白名单处理；
     // 这里不能整条排除 /products/{id}，否则 PUT/DELETE 也绕过鉴权，BaseContext 为空。
//        "/auth/**",uw
//                "/products",
//                "/products/{id}",
//                "/categories",
//                "/posts"
    }

}
