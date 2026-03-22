package com.sintao.system.domain.exam.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;

@Getter
@Setter
public class ExamQuestAddDTO {

    private Long examId;

    private LinkedHashSet<Long> questionIdSet;
}

