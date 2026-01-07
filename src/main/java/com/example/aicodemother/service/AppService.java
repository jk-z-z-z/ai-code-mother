package com.example.aicodemother.service;

import com.example.aicodemother.model.dto.app.AppFeaturedQueryRequest;
import com.example.aicodemother.model.dto.app.AppMyQueryRequest;
import com.example.aicodemother.model.dto.app.AppQueryRequest;
import com.example.aicodemother.model.entity.App;
import com.example.aicodemother.model.entity.User;
import com.example.aicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author zzZ.
 */
public interface AppService extends IService<App> {

    String deployApp(Long appId, User loginUser);

    AppVO getAppVO(App app);

    List<AppVO> getAppVOList(List<App> appList);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    boolean deleteAppAndHistory(Long appId);

    Flux<String> chatToGenCode(String message, User loginUser, Long appId);
    @Deprecated
    QueryWrapper getMyAppQueryWrapper(AppMyQueryRequest appMyQueryRequest, Long userId);

    @Deprecated
    QueryWrapper getFeaturedAppQueryWrapper(AppFeaturedQueryRequest appFeaturedQueryRequest);

}
