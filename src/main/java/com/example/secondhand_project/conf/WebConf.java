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
     registry.addInterceptor(interceptor).excludePathPatterns("/auth/**");

    }
}
