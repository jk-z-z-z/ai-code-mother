package com.example.aicodemother.service.impl;



import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aicodemother.constant.UserConstant;
import com.example.aicodemother.exception.BusinessException;
import com.example.aicodemother.exception.ErrorCode;
import com.example.aicodemother.mapper.UserMapper;
import com.example.aicodemother.model.dto.user.UserLoginRequest;
import com.example.aicodemother.model.dto.user.UserQueryRequest;
import com.example.aicodemother.model.dto.user.UserRegisterRequest;
import com.example.aicodemother.model.entity.User;
import com.example.aicodemother.model.enums.UserRoleEnum;
import com.example.aicodemother.model.vo.LoginUserVo;
import com.example.aicodemother.model.vo.UserVO;
import com.example.aicodemother.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cn.hutool.core.bean.BeanUtil.copyProperties;

/**
 * 用户 服务层实现。
 *
 * @author zzZ.
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService {

    @Override
    public Long register(UserRegisterRequest userRegisterRequest) {
        //1.校验
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if(StrUtil.hasBlank(userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if(!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次密码不一致");
        }
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        if(userPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        //2.查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount",userAccount);
        long n = this.mapper.selectCountByQuery(queryWrapper);
        if(n>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号已存在");
        }
        //3.密码加密
        String password=getEncryptPassword(userPassword);
        //4.保存用户信息
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(password);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setUserName(userAccount);
        boolean result=this.save(user);
        if(!result){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"数据库异常");
        }
        return user.getId();
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        final String salt = "123456";
        return DigestUtils.md5DigestAsHex((salt+userPassword+salt).getBytes());
    }

    @Override
    public LoginUserVo login(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        //1.校验
        String userAccount= userLoginRequest.getUserAccount();
        String userPassword= userLoginRequest.getUserPassword();
        if(StrUtil.hasBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",getEncryptPassword(userPassword));
        User user=this.getOne(queryWrapper);
        if(user==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号不存在或密码错误");
        }
        //2.记录用户登陆态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,user);
        //3.vo封装
        LoginUserVo userLoginVo = new LoginUserVo();
        copyProperties(user,userLoginVo);
        //4.返回用户信息
        return userLoginVo;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if(attribute==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return (User) attribute;
    }

    @Override
    public boolean logout(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if(attribute==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"用户未登录");
        }
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }


}
