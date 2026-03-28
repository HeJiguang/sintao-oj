package com.sintao.friend.domain.training;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.sintao.common.core.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("tb_training_plan")
public class TrainingPlan extends BaseEntity {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "PLAN_ID", type = IdType.ASSIGN_ID)
    private Long planId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String planTitle;

    private String planGoal;

    private String sourceType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long basedOnExamId;

    private Integer planStatus;

    private String aiSummary;
}
