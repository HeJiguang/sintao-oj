package com.sintao.friend.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sintao.friend.domain.exam.vo.ExamRankVO;
import com.sintao.friend.domain.exam.vo.ExamVO;
import com.sintao.friend.domain.user.UserExam;

import java.util.List;


public interface UserExamMapper extends BaseMapper<UserExam> {

    List<ExamVO> selectUserExamList(Long userId);

    List<ExamRankVO> selectExamRankList(Long examId);

}
