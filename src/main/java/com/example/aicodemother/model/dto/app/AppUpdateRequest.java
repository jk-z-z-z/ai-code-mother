package com.example.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用更新请求类，用于封装应用更新相关的请求参数
 * 实现了Serializable接口，支持序列化操作
 */
@Data  // 使用Lombok注解自动生成getter、setter等方法
public class AppUpdateRequest implements Serializable {

    // 应用ID，用于唯一标识一个应用
    private Long id;

    // 应用名称，展示给用户的应用标题
    private String appName;

    // 应用封面图片的URL地址
    private String cover;

    // 应用优先级，用于控制显示顺序
    private Integer priority;

    // 序列化版本UID，用于序列化和反序列化时的版本控制
    private static final long serialVersionUID = 1L;
}

