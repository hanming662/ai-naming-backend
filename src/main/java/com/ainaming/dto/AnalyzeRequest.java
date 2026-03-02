package com.ainaming.dto;

import lombok.Data;

@Data
public class AnalyzeRequest {
    private String fullName;
    private String birthDate;
    private String birthTime;
}