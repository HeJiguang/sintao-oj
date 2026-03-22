package com.sintao.friend.service.exam;

import com.sintao.common.core.domain.TableDataInfo;
import com.sintao.friend.domain.exam.dto.ExamQueryDTO;
import com.sintao.friend.domain.exam.dto.ExamRankDTO;
import com.sintao.friend.domain.exam.vo.ExamVO;

import java.util.List;

public interface IExamService {

    List<ExamVO> list(ExamQueryDTO examQueryDTO);

    TableDataInfo redisList(ExamQueryDTO examQueryDTO);

    TableDataInfo rankList(ExamRankDTO examRankDTO);

    String getFirstQuestion(Long examId);

    String preQuestion(Long examId, Long questionId);

    String nextQuestion(Long examId, Long questionId);
}

