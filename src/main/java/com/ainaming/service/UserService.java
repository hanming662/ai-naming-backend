package com.ainaming.service;

import com.ainaming.entity.User;
import com.ainaming.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    @Value("${app.free-generate-times:3}")
    private int freeGenerateTimes;

    /**
     * 根据 openid 获取用户，不存在则创建
     */
    public User getOrCreateUser(String openid) {
        // 使用 LambdaQueryWrapper 查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getOpenid, openid);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname("");
            user.setFreeTimesToday(0);
            user.setIsVip(false);
            user.setLastUseDate(LocalDateTime.now());
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }

        return user;
    }

    /**
     * 检查用户配额
     */
    public boolean checkQuota(String openid) {
        User user = getOrCreateUser(openid);

        // VIP 用户不限次数
        if (Boolean.TRUE.equals(user.getIsVip())
                && user.getVipExpireAt() != null
                && user.getVipExpireAt().isAfter(LocalDateTime.now())) {
            return true;
        }

        // 重置每日次数
        if (user.getLastUseDate() != null
                && user.getLastUseDate().toLocalDate().isBefore(LocalDate.now())) {
            user.setFreeTimesToday(0);
            user.setLastUseDate(LocalDateTime.now());
            userMapper.updateById(user);
        }

        // 检查免费次数
        if (user.getFreeTimesToday() == null) {
            user.setFreeTimesToday(0);
        }

        if (user.getFreeTimesToday() < freeGenerateTimes) {
            user.setFreeTimesToday(user.getFreeTimesToday() + 1);
            user.setLastUseDate(LocalDateTime.now());
            userMapper.updateById(user);
            return true;
        }

        return false;
    }

    /**
     * 获取剩余免费次数
     */
    public int getRemaining(String openid) {
        User user = getOrCreateUser(openid);

        if (user.getLastUseDate() != null
                && user.getLastUseDate().toLocalDate().isBefore(LocalDate.now())) {
            return freeGenerateTimes;
        }

        int used = user.getFreeTimesToday() != null ? user.getFreeTimesToday() : 0;
        return Math.max(0, freeGenerateTimes - used);
    }
}