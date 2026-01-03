package com.example.aicodemother.model.dto.user;

import lombok.Data;


import java.io.Serializable;

/**
 * 用户注册请求类
 * 用于封装用户注册时所需的请求参数信息
 * 实现了Serializable接口以支持序列化操作
 */
@Data  // 使用Lombok注解自动生成getter、setter等方法
public class UserRegisterRequest implements Serializable {

    /**
     * 序列化版本UID
     * 用于序列化和反序列化过程中验证版本一致性
     */
    private static final long serialVersionUID = 1L;

    /**
     * 用户账号
     * 用于存储用户的唯一标识符
     */
    private String userAccount;
    /**
     * 用户密码
     * 用于存储用户设置的密码
     */
    private String userPassword;
    /**
     * 确认密码
     * 用于验证用户输入的密码是否一致
     */
    private String checkPassword;

    // Getters and Setters  // 此处省略了getter和setter方法的注释，因为使用了Lombok的@Data注解自动生成
}
