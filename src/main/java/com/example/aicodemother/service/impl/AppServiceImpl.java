package com.example.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aicodemother.exception.BusinessException;
import com.example.aicodemother.exception.ErrorCode;
import com.example.aicodemother.mapper.AppMapper;
import com.example.aicodemother.model.dto.app.AppFeaturedQueryRequest;
import com.example.aicodemother.model.dto.app.AppMyQueryRequest;
import com.example.aicodemother.model.dto.app.AppQueryRequest;
import com.example.aicodemother.model.entity.App;
import com.example.aicodemother.model.entity.User;
import com.example.aicodemother.model.vo.AppVO;
import com.example.aicodemother.model.vo.UserVO;
import com.example.aicodemother.service.AppService;
import com.example.aicodemother.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 应用 服务层实现。
 *
 * @author zzZ.
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }

        Set<Long> userIdSet=appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User>  userMap=userIdSet.stream()
                .collect(Collectors.toMap(
                        userId -> userId,
                        userId ->userService.getById(userId)));
        return appList.stream()
                .map(app -> {
                    AppVO appVO = getAppVO(app);
                    User user = userMap.get(app.getUserId());
                    appVO.setUserVO(userService.getUserVO(user));
                    return appVO;
                }).toList();
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        Integer isDelete = appQueryRequest.getIsDelete();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .eq("isDelete", isDelete)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    @Deprecated
    public QueryWrapper getMyAppQueryWrapper(AppMyQueryRequest appMyQueryRequest, Long userId) {
        if (appMyQueryRequest == null || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String appName = appMyQueryRequest.getAppName();
        String sortField = appMyQueryRequest.getSortField();
        String sortOrder = appMyQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("userId", userId)
                .like("appName", appName)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }
    @Override
    @Deprecated
    public QueryWrapper getFeaturedAppQueryWrapper(AppFeaturedQueryRequest appFeaturedQueryRequest) {
        if (appFeaturedQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String appName = appFeaturedQueryRequest.getAppName();
        String sortField = appFeaturedQueryRequest.getSortField();
        String sortOrder = appFeaturedQueryRequest.getSortOrder();
        QueryWrapper queryWrapper = QueryWrapper.create()
                .gt("priority", 0)
                .like("appName", appName);
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            queryWrapper.orderBy("priority", false).orderBy("createTime", false);
        }
        return queryWrapper;
    }
}
