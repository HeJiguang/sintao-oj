package com.sintao.job.domain.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserScore {

    private Long examId;

    private Long userId;

    private int score;

    private int examRank;
}

