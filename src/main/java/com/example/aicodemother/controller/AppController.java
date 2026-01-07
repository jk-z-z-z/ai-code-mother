package com.example.aicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.aicodemother.annotation.AuthCheck;
import com.example.aicodemother.common.BaseResponse;
import com.example.aicodemother.common.DeleteRequest;
import com.example.aicodemother.common.ResultUtils;
import com.example.aicodemother.constant.AppConstant;
import com.example.aicodemother.constant.UserConstant;
import com.example.aicodemother.exception.BusinessException;
import com.example.aicodemother.exception.ErrorCode;
import com.example.aicodemother.exception.ThrowUtils;
import com.example.aicodemother.model.dto.app.*;
import com.example.aicodemother.model.entity.App;
import com.example.aicodemother.model.entity.User;
import com.example.aicodemother.model.vo.AppVO;
import com.example.aicodemother.service.AppService;
import com.example.aicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author zzZ.
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Autowired
    private AppService appService;

    @Autowired
    private UserService userService;

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }


    @GetMapping("/chat/gen/code")
    public Flux<ServerSentEvent<String>> chatToGenCode(String message, Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "message不能为空");
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "appId不能为空");
        User loginUser = userService.getLoginUser(request);
        Flux<String> contentFlux = appService.chatToGenCode(message, loginUser, appId);
        return contentFlux
                .map(chunk -> {
                    Map<Character, String> map = Map.of('d', chunk);
                    String jsonData = JSONUtil.toJsonStr(map);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("")
                        .build()
                ));
    }

    /**
     * 添加应用接口
     *
     * @param appAddRequest 添加应用的请求参数
     * @param request       HTTP请求对象
     * @return 返回新添加应用的ID
     */
    @Operation(description = "用户添加应用接口")
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        // 检查请求参数是否为空
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 获取初始化提示词并检查是否为空
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "initPrompt不能为空");
        // 获取当前登录用户信息
        User loginUser = userService.getLoginUser(request);
        // 创建新应用对象并复制请求参数
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        // 设置用户ID和默认优先级
        app.setUserId(loginUser.getId());
        if(app.getCover() == null){
            app.setCover(loginUser.getUserAvatar());
        }
        app.setAppName(initPrompt.substring(0, Math.min(10, initPrompt.length())));
        if (app.getPriority() == null) {
            app.setPriority(0);
        }
        // 保存应用信息
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回成功响应，包含新应用的ID
        return ResultUtils.success(app.getId());
    }

    /**
     * 更新当前用户的应用信息
     *
     * @param appUpdateMyRequest 包含要更新的应用信息的请求对象
     * @param request            HTTP请求对象，用于获取当前登录用户信息
     * @return BaseResponse<Boolean> 更新操作是否成功的结果
     */
    @Operation(description = "用户更新应用接口")
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyApp(@RequestBody AppUpdateMyRequest appUpdateMyRequest, HttpServletRequest request) {
        // 检查请求参数或应用ID是否为空，若为空则抛出参数错误异常
        ThrowUtils.throwIf(appUpdateMyRequest == null || appUpdateMyRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        // 检查请求对象是否为空，若为空则抛出参数错误异常
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 获取应用名称
        String appName = appUpdateMyRequest.getAppName();
        // 检查应用名称是否为空，若为空则抛出参数错误异常并提示"应用名称不能为空"
        ThrowUtils.throwIf(StrUtil.isBlank(appName), ErrorCode.PARAMS_ERROR, "应用名称不能为空");
        // 获取当前登录用户信息
        User loginUser = userService.getLoginUser(request);
        // 根据应用ID获取应用信息
        App oldApp = appService.getById(appUpdateMyRequest.getId());
        // 检查应用是否存在，若不存在则抛出未找到错误异常
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 检查当前用户是否有权限更新该应用，若无权限则抛出无权限错误异常
        ThrowUtils.throwIf(!loginUser.getId().equals(oldApp.getUserId()), ErrorCode.NO_AUTH_ERROR);
        // 创建新的应用对象并设置ID和名称
        App app = new App();
        app.setId(appUpdateMyRequest.getId());
        app.setAppName(appName);
        app.setCover(appUpdateMyRequest.getCover());
        // 更新应用信息到数据库，并获取更新结果
        boolean result = appService.updateById(app);
        // 检查更新是否成功，若失败则抛出操作错误异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回更新成功的结果
        return ResultUtils.success(true);
    }

    /**
     * 删除当前用户的应用接口
     *
     * @param deleteRequest 包含要删除的应用ID的请求体
     * @param request       HTTP请求对象，用于获取当前登录用户信息
     * @return 返回操作结果，成功返回true，失败则抛出相应异常
     */
    @Operation(description = "用户删除应用接口")
    @PostMapping("/delete/my")
    public BaseResponse<Boolean> deleteMyApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 检查请求参数是否有效，包括请求体本身和ID的合法性
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 检查HTTP请求对象是否存在
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户信息
        User loginUser = userService.getLoginUser(request);
        // 根据ID查询要删除的应用
        App oldApp = appService.getById(deleteRequest.getId());
        // 检查应用是否存在
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 检查当前用户是否有权限删除该应用（只有应用创建者才能删除）
        ThrowUtils.throwIf((!loginUser.getId().equals(oldApp.getUserId())) || !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()), ErrorCode.NO_AUTH_ERROR);
        // 执行删除操作（包含关联对话历史）
        boolean result = appService.deleteAppAndHistory(deleteRequest.getId());
        // 检查删除操作是否成功
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回操作成功结果
        return ResultUtils.success(true);
    }

    /**
     * 根据ID查询用户的应用接口
     * 该接口用于获取指定ID的应用信息，并进行权限校验
     *
     * @param id      应用ID，必须为大于0的正整数
     * @param request HTTP请求对象，用于获取当前登录用户信息
     * @return BaseResponse<AppVO> 返回应用信息的视图对象
     */
    @Operation(description = "用户查询应用接口")
    @GetMapping("/get/my")
    public BaseResponse<AppVO> getMyAppById(@RequestParam("id") Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        User user = userService.getById(app.getUserId());
        ThrowUtils.throwIf(!loginUser.getId().equals(user.getId()), ErrorCode.NO_AUTH_ERROR);
        AppVO appVO = appService.getAppVO(app);
        appVO.setUserVO(userService.getUserVO(user));
        return ResultUtils.success(appVO);
    }

    /**
     * 用户批量查询应用接口
     * 该接口用于分页查询当前用户的应用列表
     *
     * @param appQueryRequest 应用查询请求参数，包含分页信息和查询条件
     * @param request         HTTP请求对象，用于获取用户登录信息
     * @return 返回分页后的应用视图对象列表
     */
    @Operation(description = "用户批量查询应用接口")
    @PostMapping("/my/list/page")
    public BaseResponse<Page<AppVO>> listMyAppByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        appQueryRequest.setUserId(loginUser.getId());
        int pageNum = appQueryRequest.getPageNum();
        int pageSize = appQueryRequest.getPageSize();
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页查询数量不能超过20");
        }
        ThrowUtils.throwIf(pageNum <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    @Operation(description = "用户查询精选应用接口")
    @PostMapping("/featured/list/page")
    public BaseResponse<Page<AppVO>> listFeaturedAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页查询数量不能超过20");
        }
        ThrowUtils.throwIf(pageNum <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    @Operation(description = "管理员更新应用接口")
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest) {
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appUpdateRequest, app);
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员删除应用接口
     * 该接口用于根据应用ID删除应用
     *
     * @param deleteRequest 删除请求参数，包含要删除的应用ID
     * @return 返回删除操作是否成功响应对象
     */
    @Operation(description = "管理员删除应用接口")
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.deleteAppAndHistory(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @Operation(description = "管理员批量查询应用接口")
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageNum <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    @Operation(description = "管理员查询应用接口")
    @GetMapping("/admin/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppById(@RequestParam("id") Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        AppVO appVO = appService.getAppVO(app);
        return ResultUtils.success(appVO);
    }
}
