package com.ainaming.dto;

import lombok.Data;

@Data
public class NamingRequest {
    private String openid;
    private String surname;
    private String gender;
    private String birthDate;
    private String birthTime;
    private String style = "现代文雅";
    private String meaningPreference = "";
    private String avoidChars = "";
    private String preferChars = "";
    private Integer charCount = 2;
}