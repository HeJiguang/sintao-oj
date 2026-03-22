package com.sintao.system.mapper.exam;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sintao.system.domain.exam.ExamQuestion;
import com.sintao.system.domain.question.vo.QuestionVO;

import java.util.List;

public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {

    List<QuestionVO> selectExamQuestionList(Long examId);
}

