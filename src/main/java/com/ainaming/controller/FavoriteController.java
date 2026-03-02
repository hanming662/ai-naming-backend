package com.ainaming.controller;

import com.ainaming.dto.ApiResponse;
import com.ainaming.dto.FavoriteRequest;
import com.ainaming.entity.User;
import com.ainaming.entity.UserFavorite;
import com.ainaming.mapper.UserFavoriteMapper;
import com.ainaming.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final UserFavoriteMapper favoriteMapper;
    private final UserService userService;

    @PostMapping("/add")
    public ApiResponse<String> addFavorite(@RequestBody FavoriteRequest req) {
        User user = userService.getOrCreateUser(req.getOpenid());

        UserFavorite fav = new UserFavorite();
        fav.setUserId(user.getId());
        fav.setFullName(req.getFullName());
        fav.setNotes(req.getNotes() != null ? req.getNotes() : "");
        fav.setCreatedAt(LocalDateTime.now());

        favoriteMapper.insert(fav);

        return ApiResponse.success("收藏成功", null);
    }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> listFavorites(@RequestParam String openid) {
        User user = userService.getOrCreateUser(openid);

        // 使用 LambdaQueryWrapper 查询
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, user.getId())
                .orderByDesc(UserFavorite::getCreatedAt);

        List<UserFavorite> favs = favoriteMapper.selectList(wrapper);

        List<Map<String, Object>> items = favs.stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("full_name", f.getFullName());
            m.put("notes", f.getNotes());
            m.put("created_at", f.getCreatedAt() != null ? f.getCreatedAt().toString() : "");
            return m;
        }).collect(Collectors.toList());

        return ApiResponse.success(items);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteFavorite(@PathVariable Long id) {
        favoriteMapper.deleteById(id);
        return ApiResponse.success("已删除", null);
    }
}