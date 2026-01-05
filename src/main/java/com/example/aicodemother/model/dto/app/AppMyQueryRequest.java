package com.example.aicodemother.model.dto.app;

import com.example.aicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AppMyQueryRequest 类，用于封装应用查询请求参数
 * 继承自PageRequest类并实现Serializable接口，支持序列化
 * 使用Lombok注解简化代码，包括自动生成equals、hashCode、getter和setter方法
 */
@Deprecated
@EqualsAndHashCode(callSuper = true)  // 生成equals和hashCode方法时，会考虑父类的字段
@Data  // Lombok注解，自动生成getter、setter、toString等方法
public class AppMyQueryRequest extends PageRequest implements Serializable {

    private String appName;  // 应用名称属性，用于查询条件

    private static final long serialVersionUID = 1L;  // 序列化版本ID，用于版本控制
}

