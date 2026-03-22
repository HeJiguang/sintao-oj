package com.sintao.friend.domain.exam.dto;

import com.sintao.common.core.domain.PageQueryDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamQueryDTO extends PageQueryDTO {

    private String title;

    private String startTime;

    private String endTime;

    private Integer type; //0 未完�? 1 历史竞赛
}

