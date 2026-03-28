package com.sintao.friend.domain.training.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TrainingTaskVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

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

    private String resultStatus;

    private Integer resultScore;

    private Integer resultRank;

    private LocalDateTime resultUpdatedAt;
}
