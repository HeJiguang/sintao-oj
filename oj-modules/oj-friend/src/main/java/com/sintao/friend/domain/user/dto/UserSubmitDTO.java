package com.sintao.friend.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSubmitDTO {

    private Long examId;  //可�?
    private Long questionId;

    private Integer programType;  // (0: java  1:cpp 2: golang)

    private String userCode;
}

