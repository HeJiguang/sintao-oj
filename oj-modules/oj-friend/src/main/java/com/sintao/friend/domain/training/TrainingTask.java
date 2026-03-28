package com.sintao.friend.domain.training;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.sintao.common.core.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("tb_training_task")
public class TrainingTask extends BaseEntity {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "TASK_ID", type = IdType.ASSIGN_ID)
    private Long taskId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long planId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String taskType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    private String titleSnapshot;

    private Integer taskOrder;

    private Integer taskStatus;

    private String recommendedReason;

    private String knowledgeTagsSnapshot;

    private LocalDateTime dueTime;

    @TableField(exist = false)
    private String resultStatus;

    @TableField(exist = false)
    private Integer resultScore;

    @TableField(exist = false)
    private Integer resultRank;

    @TableField(exist = false)
    private LocalDateTime resultUpdatedAt;
}
