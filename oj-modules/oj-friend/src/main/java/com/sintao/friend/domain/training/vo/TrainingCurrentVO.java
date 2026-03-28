package com.sintao.friend.domain.training.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TrainingCurrentVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long planId;

    private String planTitle;

    private String planGoal;

    private Integer planStatus;

    private String aiSummary;

    private String currentLevel;

    private String targetDirection;

    private String weakPoints;

    private String strongPoints;

    private List<TrainingTaskVO> tasks = new ArrayList<>();
}
