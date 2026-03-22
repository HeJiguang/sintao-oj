package com.sintao.friend.service.question;

import com.sintao.common.core.domain.TableDataInfo;
import com.sintao.friend.domain.question.dto.QuestionQueryDTO;
import com.sintao.friend.domain.question.vo.QuestionDetailVO;
import com.sintao.friend.domain.question.vo.QuestionVO;

import java.util.List;

public interface IQuestionService {

    TableDataInfo list(QuestionQueryDTO questionQueryDTO);

    List<QuestionVO> hotList();

    QuestionDetailVO detail(Long questionId);

    String preQuestion(Long questionId);

    String nextQuestion(Long questionId);
}

