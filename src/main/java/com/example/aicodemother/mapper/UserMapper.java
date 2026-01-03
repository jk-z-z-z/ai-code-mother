package com.example.aicodemother.mapper;

import com.example.aicodemother.model.entity.User;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 映射层。
 *
 * @author zzZ.
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
