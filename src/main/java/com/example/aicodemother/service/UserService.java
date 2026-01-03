package com.example.aicodemother.service;

import com.example.aicodemother.model.dto.user.UserLoginRequest;
import com.example.aicodemother.model.dto.user.UserQueryRequest;
import com.example.aicodemother.model.dto.user.UserRegisterRequest;
import com.example.aicodemother.model.entity.User;
import com.example.aicodemother.model.vo.LoginUserVo;
import com.example.aicodemother.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author zzZ.
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求对象
     * @return 注册成功后的用户ID
     */
    Long register(UserRegisterRequest userRegisterRequest);

    String getEncryptPassword(String userPassword);

    LoginUserVo login(UserLoginRequest userLoginRequest, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    boolean logout(HttpServletRequest request);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}
