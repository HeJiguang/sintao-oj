package com.sintao.friend.service.user;

import com.sintao.common.core.domain.TableDataInfo;
import com.sintao.friend.domain.exam.dto.ExamQueryDTO;

public interface IUserExamService {

    int enter(String token, Long examId);

    TableDataInfo list(ExamQueryDTO examQueryDTO);
}

