package com.sintao.friend.domain.question.dto;

import com.sintao.common.core.domain.PageQueryDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionQueryDTO extends PageQueryDTO {

    private String keyword;

    private Integer difficulty;
}

