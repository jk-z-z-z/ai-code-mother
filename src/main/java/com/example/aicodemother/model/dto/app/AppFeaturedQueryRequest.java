package com.example.aicodemother.model.dto.app;

import com.example.aicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AppFeaturedQueryRequest 类
 * 继承自PageRequest并实现Serializable接口，用于应用特色查询请求
 * 使用Lombok的@EqualsAndHashCode和@Data注解简化代码
 */
@Deprecated
@EqualsAndHashCode(callSuper = true) // 继承父类的equals和hashCode方法
@Data // 使用Lombok自动生成getter、setter、toString等方法
public class AppFeaturedQueryRequest extends PageRequest implements Serializable {

    private String appName; // 应用名称属性

    private static final long serialVersionUID = 1L; // 序列化版本ID，用于序列化和反序列化
}

