package com.sintao.friend.service.user;

import com.sintao.api.domain.vo.UserQuestionResultVO;
import com.sintao.common.core.domain.R;
import com.sintao.friend.domain.user.dto.UserSubmitDTO;

public interface IUserQuestionService {
    R<UserQuestionResultVO> submit(UserSubmitDTO submitDTO);

    boolean rabbitSubmit(UserSubmitDTO submitDTO);

    UserQuestionResultVO exeResult(Long examId, Long questionId, String currentTime);
}

