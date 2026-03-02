package com.ainaming.dto;

import lombok.Data;

@Data
public class FavoriteRequest {
    private String openid;
    private String fullName;
    private Integer totalScore = 0;
    private String notes = "";
}