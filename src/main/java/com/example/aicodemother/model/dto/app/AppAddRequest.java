package com.example.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用添加请求实体类
 * 实现了Serializable接口，支持序列化操作
 * 使用@Data注解自动生成getter、setter等方法
 */
@Data
public class AppAddRequest implements Serializable {


    private String initPrompt; // 初始化提示信息

    private String codeGenType; // 代码生成类型

    // 序列化版本号，用于控制版本兼容性
    private static final long serialVersionUID = 1L;
}

