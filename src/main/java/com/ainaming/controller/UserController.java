package com.ainaming.controller;

import com.ainaming.dto.ApiResponse;
import com.ainaming.entity.User;
import com.ainaming.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getUserInfo(@RequestParam String openid) {
        User user = userService.getOrCreateUser(openid);
        int remaining = userService.getRemaining(openid);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("openid", openid);
        data.put("is_vip", user.getIsVip());
        data.put("free_times_remaining", remaining);
        return ApiResponse.success(data);
    }
}