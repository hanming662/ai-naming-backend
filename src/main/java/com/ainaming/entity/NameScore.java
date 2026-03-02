package com.ainaming.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("name_scores")
public class NameScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fullName;

    private String surname;

    private String givenName;

    private String gender;

    private Integer totalScore;

    private Integer meaningScore;

    private Integer soundScore;

    private Integer wuxingScore;

    private Integer sancaiScore;

    private Integer cultureScore;

    private Integer modernityScore;

    private String analysisText;

    private String aiComment;

    private String charactersDetail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}