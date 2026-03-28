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
@TableName("tb_training_profile")
public class TrainingProfile extends BaseEntity {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "PROFILE_ID", type = IdType.ASSIGN_ID)
    private Long profileId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String currentLevel;

    private String targetDirection;

    private String weakPoints;

    private String strongPoints;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long lastTestExamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long lastPlanId;

    private Integer status;
}
