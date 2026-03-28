package com.sintao.friend.service.user.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sintao.api.RemoteJudgeService;
import com.sintao.api.domain.UserExeResult;
import com.sintao.api.domain.dto.JudgeSubmitDTO;
import com.sintao.api.domain.vo.UserQuestionResultVO;
import com.sintao.common.core.constants.Constants;
import com.sintao.common.core.domain.R;
import com.sintao.common.core.enums.JudgeAsyncStatus;
import com.sintao.common.core.enums.ProgramType;
import com.sintao.common.core.enums.QuestionResType;
import com.sintao.common.core.enums.ResultCode;
import com.sintao.common.core.utils.ThreadLocalUtil;
import com.sintao.common.redis.service.JudgeRuntimeStateService;
import com.sintao.common.security.exception.ServiceException;
import com.sintao.friend.domain.question.Question;
import com.sintao.friend.domain.question.QuestionCase;
import com.sintao.friend.domain.question.es.QuestionES;
import com.sintao.friend.domain.user.UserSubmit;
import com.sintao.friend.domain.user.dto.UserSubmitDTO;
import com.sintao.friend.domain.user.vo.AsyncSubmitResponseVO;
import com.sintao.friend.domain.user.vo.UserSubmissionHistoryVO;
import com.sintao.friend.elasticsearch.QuestionRepository;
import com.sintao.friend.mapper.question.QuestionMapper;
import com.sintao.friend.mapper.user.UserSubmitMapper;
import com.sintao.friend.rabbit.JudgeProducer;
import com.sintao.friend.service.user.IUserQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserQuestionServiceImpl implements IUserQuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private RemoteJudgeService remoteJudgeService;

    @Autowired
    private JudgeProducer judgeProducer;

    @Autowired
    private JudgeRuntimeStateService judgeRuntimeStateService;

    @Override
    public R<UserQuestionResultVO> submit(UserSubmitDTO submitDTO) {
        Integer programType = submitDTO.getProgramType();
        if (ProgramType.JAVA.getValue().equals(programType)) {
            JudgeSubmitDTO judgeSubmitDTO = assembleJudgeSubmitDTO(submitDTO);
            return remoteJudgeService.doJudgeJavaCode(judgeSubmitDTO);
        }
        throw new ServiceException(ResultCode.FAILED_NOT_SUPPORT_PROGRAM);
    }

    @Override
    public AsyncSubmitResponseVO rabbitSubmit(UserSubmitDTO submitDTO) {
        Integer programType = submitDTO.getProgramType();
        if (ProgramType.JAVA.getValue().equals(programType)) {
            JudgeSubmitDTO judgeSubmitDTO = assembleJudgeSubmitDTO(submitDTO);
            judgeSubmitDTO.setRequestId(UUID.randomUUID().toString().replace("-", ""));
            userSubmitMapper.insert(buildAcceptedSubmit(judgeSubmitDTO));
            judgeRuntimeStateService.markAccepted(judgeSubmitDTO.getRequestId());
            judgeProducer.produceMsg(judgeSubmitDTO);
            AsyncSubmitResponseVO responseVO = new AsyncSubmitResponseVO();
            responseVO.setRequestId(judgeSubmitDTO.getRequestId());
            responseVO.setStatus("ACCEPTED");
            return responseVO;
        }
        throw new ServiceException(ResultCode.FAILED_NOT_SUPPORT_PROGRAM);
    }

    @Override
    public UserQuestionResultVO exeResult(Long examId, Long questionId, String currentTime, String requestId) {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        UserSubmit userSubmit = getCurrentSubmit(userId, examId, questionId, currentTime, requestId);
        UserQuestionResultVO resultVO = new UserQuestionResultVO();
        if (userSubmit == null) {
            resultVO.setPass(QuestionResType.IN_JUDGE.getValue());
        } else {
            resultVO.setPass(userSubmit.getPass());
            resultVO.setExeMessage(userSubmit.getExeMessage());
            if (StrUtil.isNotEmpty(userSubmit.getCaseJudgeRes())) {
                resultVO.setUserExeResultList(JSON.parseArray(userSubmit.getCaseJudgeRes(), UserExeResult.class));
            }
        }
        return resultVO;
    }

    @Override
    public List<UserSubmissionHistoryVO> submissionHistory(Long examId, Long questionId) {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        return userSubmitMapper.selectSubmissionHistory(userId, examId, questionId, 20).stream()
                .map(this::toSubmissionHistoryVO)
                .collect(Collectors.toList());
    }

    private JudgeSubmitDTO assembleJudgeSubmitDTO(UserSubmitDTO submitDTO) {
        Long questionId = submitDTO.getQuestionId();
        QuestionES questionES = questionRepository.findById(questionId).orElse(null);
        JudgeSubmitDTO judgeSubmitDTO = new JudgeSubmitDTO();
        if (questionES != null) {
            BeanUtil.copyProperties(questionES, judgeSubmitDTO);
        } else {
            Question question = questionMapper.selectById(questionId);
            BeanUtil.copyProperties(question, judgeSubmitDTO);
            questionES = new QuestionES();
            BeanUtil.copyProperties(question, questionES);
            questionRepository.save(questionES);
        }
        judgeSubmitDTO.setUserId(ThreadLocalUtil.get(Constants.USER_ID, Long.class));
        judgeSubmitDTO.setExamId(submitDTO.getExamId());
        judgeSubmitDTO.setProgramType(submitDTO.getProgramType());
        judgeSubmitDTO.setUserCode(codeConnect(submitDTO.getUserCode(), questionES.getMainFuc()));
        List<QuestionCase> questionCaseList = JSONUtil.toList(questionES.getQuestionCase(), QuestionCase.class);
        List<String> inputList = questionCaseList.stream().map(QuestionCase::getInput).toList();
        judgeSubmitDTO.setInputList(inputList);
        List<String> outputList = questionCaseList.stream().map(QuestionCase::getOutput).toList();
        judgeSubmitDTO.setOutputList(outputList);
        return judgeSubmitDTO;
    }

    private String codeConnect(String userCode, String mainFunc) {
        String targetCharacter = "}";
        int targetLastIndex = userCode.lastIndexOf(targetCharacter);
        if (targetLastIndex != -1) {
            return userCode.substring(0, targetLastIndex) + "\n" + mainFunc + "\n" + userCode.substring(targetLastIndex);
        }
        throw new ServiceException(ResultCode.FAILED);
    }

    private UserSubmit buildAcceptedSubmit(JudgeSubmitDTO judgeSubmitDTO) {
        UserSubmit userSubmit = new UserSubmit();
        userSubmit.setRequestId(judgeSubmitDTO.getRequestId());
        userSubmit.setUserId(judgeSubmitDTO.getUserId());
        userSubmit.setQuestionId(judgeSubmitDTO.getQuestionId());
        userSubmit.setExamId(judgeSubmitDTO.getExamId());
        userSubmit.setProgramType(judgeSubmitDTO.getProgramType());
        userSubmit.setUserCode(judgeSubmitDTO.getUserCode());
        userSubmit.setPass(QuestionResType.IN_JUDGE.getValue());
        userSubmit.setScore(0);
        userSubmit.setExeMessage("判题中");
        userSubmit.setRetryCount(0);
        userSubmit.setJudgeStatus(JudgeAsyncStatus.WAITING.getValue());
        userSubmit.setCreateBy(judgeSubmitDTO.getUserId());
        return userSubmit;
    }

    private UserSubmit getCurrentSubmit(Long userId,
                                        Long examId,
                                        Long questionId,
                                        String currentTime,
                                        String requestId) {
        if (StrUtil.isNotBlank(requestId)) {
            return userSubmitMapper.selectOne(new LambdaQueryWrapper<UserSubmit>()
                    .eq(UserSubmit::getRequestId, requestId)
                    .eq(UserSubmit::getUserId, userId));
        }
        return userSubmitMapper.selectCurrentUserSubmit(userId, examId, questionId, currentTime);
    }

    private UserSubmissionHistoryVO toSubmissionHistoryVO(UserSubmit userSubmit) {
        UserSubmissionHistoryVO historyVO = new UserSubmissionHistoryVO();
        historyVO.setSubmitId(userSubmit.getSubmitId());
        historyVO.setProgramType(userSubmit.getProgramType());
        historyVO.setPass(userSubmit.getPass());
        historyVO.setScore(userSubmit.getScore());
        historyVO.setExeMessage(userSubmit.getExeMessage());
        historyVO.setCreateTime(userSubmit.getCreateTime());
        historyVO.setUpdateTime(userSubmit.getUpdateTime());
        return historyVO;
    }
}
