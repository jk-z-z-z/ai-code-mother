package com.example.aicodemother.model.dto.app;

import com.example.aicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AppQueryRequest 类，继承自PageRequest，实现了Serializable接口
 * 用于应用程序查询请求的参数封装
 * 使用了Lombok的@Data和@EqualsAndHashCode注解简化代码
 */
@EqualsAndHashCode(callSuper = true) // 继承父类的equals和hashCode方法
@Data // 自动生成getter、setter、toString等方法
public class AppQueryRequest extends PageRequest implements Serializable {

    private Long id; // 应用ID，用于唯一标识一个应用

    private String appName; // 应用名称，用于展示和搜索

    private String cover; // 应用封面图片的URL或路径

    private String initPrompt; // 应用初始化提示信息

    private String codeGenType;

    private String deployKey;

    private Integer priority;

    private Long userId;

    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}

