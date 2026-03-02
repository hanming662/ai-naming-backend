package com.ainaming.mapper;

import com.ainaming.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // BaseMapper 已自带: insert, deleteById, updateById, selectById, selectList 等
    // 无需手写 SQL
}