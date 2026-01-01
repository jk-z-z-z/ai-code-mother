package com.example.aicodemother.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS跨域配置类
 * 实现WebMvcConfigurer接口，用于配置Spring MVC的跨域资源共享(CORS)规则
 */
@Configuration  // 标识该类为配置类，用于替代传统的XML配置文件
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 配置跨域映射规则
     * @param registry CorsRegistry对象，用于注册跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        registry.addMapping("/**")  // 配置允许跨域的路径，/**表示所有路径
                // 允许发送 Cookie
                .allowCredentials(true)  // 允许发送凭据信息（如Cookie、HTTP认证等）
                // 放行哪些域名（必须用 patterns，否则 * 会和 allowCredentials 冲突）
                .allowedOriginPatterns("*")  // 设置允许跨域请求的源模式，*表示所有域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 设置允许的HTTP方法
                .allowedHeaders("*")  // 设置允许的请求头，*表示所有请求头
                .exposedHeaders("*");  // 设置暴露给客户端的响应头，*表示所有响应头
    }
}

