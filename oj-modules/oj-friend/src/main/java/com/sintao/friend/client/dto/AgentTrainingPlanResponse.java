package com.sintao.friend.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AgentTrainingPlanResponse {

    private String currentLevel;

    private String targetDirection;

    private String weakPoints;

    private String strongPoints;

    private String planTitle;

    private String planGoal;

    private String aiSummary;

    private List<PlanTask> tasks = new ArrayList<>();

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class PlanTask {
        private String taskType;
        private Long questionId;
        private Long examId;
        private String titleSnapshot;
        private Integer taskOrder;
        private String recommendedReason;
        private String knowledgeTagsSnapshot;
        private LocalDateTime dueTime;
    }
}
