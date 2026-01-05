package com.example.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aicodemother.constant.AppConstant;
import com.example.aicodemother.cores.AiCodeGeneratorFacade;
import com.example.aicodemother.exception.BusinessException;
import com.example.aicodemother.exception.ErrorCode;
import com.example.aicodemother.exception.ThrowUtils;
import com.example.aicodemother.mapper.AppMapper;
import com.example.aicodemother.model.dto.app.AppFeaturedQueryRequest;
import com.example.aicodemother.model.dto.app.AppMyQueryRequest;
import com.example.aicodemother.model.dto.app.AppQueryRequest;
import com.example.aicodemother.model.entity.App;
import com.example.aicodemother.model.entity.User;
import com.example.aicodemother.model.enums.CodeGenTypeEnum;
import com.example.aicodemother.model.vo.AppVO;
import com.example.aicodemother.service.AppService;
import com.example.aicodemother.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
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
    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 8. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 9. 返回可访问的 URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }


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
    public Flux<String> chatToGenCode(String message, User loginUser, Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID异常");
        ThrowUtils.throwIf(message== null || StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null || loginUser.getId() <= 0, ErrorCode.NOT_LOGIN_ERROR, "用户登陆状态异常");
        App app= getById(appId);
        ThrowUtils.throwIf(app == null || app.getIsDelete() == 1, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        Long appUserId = app.getUserId();
        User user=userService.getById(appUserId);
        ThrowUtils.throwIf(user == null || user.getIsDelete() == 1, ErrorCode.NOT_FOUND_ERROR, "应用用户不存在");
        ThrowUtils.throwIf(!loginUser.getId().equals(appUserId), ErrorCode.NO_AUTH_ERROR, "用户权限异常");
        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        ThrowUtils.throwIf(codeGenType == null, ErrorCode.PARAMS_ERROR, "应用生成类型异常");
        return aiCodeGeneratorFacade.generateAndSaveCodeStreaming(message,codeGenType,appId);
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
