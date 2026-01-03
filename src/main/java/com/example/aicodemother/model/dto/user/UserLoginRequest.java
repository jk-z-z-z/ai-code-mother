package com.example.aicodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求类
 * 实现Serializable接口以支持序列化
 */
@Data
public class UserLoginRequest implements Serializable {
    /**
     * 序列化版本UID，用于版本控制
     */
    private static final long serialVersionUID = 1L;
    /**
     * 用户名属性
     */
    private String userAccount;
    /**
     * 密码属性
     */
    private String userPassword;
}
