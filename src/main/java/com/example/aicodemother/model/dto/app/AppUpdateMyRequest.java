package com.example.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * AppUpdateMyRequest 类，实现了Serializable接口
 * 用于应用程序更新请求的参数封装
 * 使用了Lombok的@Data注解简化代码
 */
@Data
public class AppUpdateMyRequest implements Serializable {
    /**
     * 应用ID，用于唯一标识一个应用
     */
    private Long id; // 应用ID，用于唯一标识一个应用

    private String appName; // 应用名称，用于展示和搜索

    private String cover;

    private static final long serialVersionUID = 1L;
}

