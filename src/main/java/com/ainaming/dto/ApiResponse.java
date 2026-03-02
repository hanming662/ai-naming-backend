package com.ainaming.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private String timestamp = LocalDateTime.now().toString();

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(200);
        resp.setMessage("成功");
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(200);
        resp.setMessage(message);
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }
}