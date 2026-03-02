package com.ainaming.dto;

import lombok.Data;
import java.util.List;

@Data
public class CompareRequest {
    private List<String> names;
    private String birthDate;
    private String birthTime;
}