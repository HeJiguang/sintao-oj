package com.sintao.system.mapper.question;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sintao.system.domain.question.Question;
import com.sintao.system.domain.question.dto.QuestionQueryDTO;
import com.sintao.system.domain.question.vo.QuestionVO;

import java.util.List;

public interface QuestionMapper extends BaseMapper<Question> {

    List<QuestionVO> selectQuestionList(QuestionQueryDTO questionQueryDTO);
}

