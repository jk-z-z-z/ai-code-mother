package com.example.aicodemother.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录响应类
 * 脱敏处理后的用户登录响应类
 * 该类实现了Serializable接口，支持序列化操作
 */
@Data
public class LoginUserVo implements Serializable {
    private static final long serialVersionUID = 1L; // 序列化版本号，用于版本控制

    private Long id; // 用户ID

    private String userAccount; // 用户账号

    private String userName; // 用户名

    private String userAvatar; // 用户头像

    private String userProfile; // 用户简介

    private String userRole; // 用户角色

    private String updateTime; // 更新时间

    private String createTime; // 创建时间
}
