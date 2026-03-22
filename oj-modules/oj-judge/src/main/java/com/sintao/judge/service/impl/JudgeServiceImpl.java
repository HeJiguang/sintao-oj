package com.sintao.judge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sintao.api.domain.UserExeResult;
import com.sintao.api.domain.dto.JudgeSubmitDTO;
import com.sintao.api.domain.vo.UserQuestionResultVO;
import com.sintao.common.core.constants.Constants;
import com.sintao.common.core.constants.JudgeConstants;
import com.sintao.common.core.enums.CodeRunStatus;
import com.sintao.judge.domain.SandBoxExecuteResult;
import com.sintao.judge.domain.UserSubmit;
import com.sintao.judge.mapper.UserSubmitMapper;
import com.sintao.judge.service.IJudgeService;
import com.sintao.judge.service.ISandboxPoolService;
import com.sintao.judge.service.ISandboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class JudgeServiceImpl implements IJudgeService {

    @Autowired
    private ISandboxService sandboxService;

    @Autowired
    private ISandboxPoolService sandboxPoolService;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Override
    public UserQuestionResultVO doJudgeJavaCode(JudgeSubmitDTO judgeSubmitDTO) {
        log.info("---- 判题逻辑开始 -------");
        SandBoxExecuteResult sandBoxExecuteResult =
                sandboxPoolService.exeJavaCode(judgeSubmitDTO.getUserId(), judgeSubmitDTO.getUserCode(), judgeSubmitDTO.getInputList());
        UserQuestionResultVO userQuestionResultVO = new UserQuestionResultVO();
        if (sandBoxExecuteResult != null && CodeRunStatus.SUCCEED.equals(sandBoxExecuteResult.getRunStatus())) {
            // 比对直接结果，以及时间限制、空间限制
            userQuestionResultVO = doJudge(judgeSubmitDTO, sandBoxExecuteResult, userQuestionResultVO);
        } else {
            userQuestionResultVO.setPass(Constants.FALSE);
            if (sandBoxExecuteResult != null) {
                userQuestionResultVO.setExeMessage(sandBoxExecuteResult.getExeMessage());
            } else {
                userQuestionResultVO.setExeMessage(CodeRunStatus.UNKNOWN_FAILED.getMsg());
            }
            userQuestionResultVO.setScore(JudgeConstants.ERROR_SCORE);
        }
        saveUserSubmit(judgeSubmitDTO, userQuestionResultVO);
        log.info("判题逻辑结束，判题结果为：{} ", userQuestionResultVO);
        return userQuestionResultVO;
    }

    private UserQuestionResultVO doJudge(JudgeSubmitDTO judgeSubmitDTO,
                                         SandBoxExecuteResult sandBoxExecuteResult,
                                         UserQuestionResultVO userQuestionResultVO) {
        List<String> exeOutputList = sandBoxExecuteResult.getOutputList();
        List<String> outputList = judgeSubmitDTO.getOutputList();
        if (outputList.size() != exeOutputList.size()) {
            userQuestionResultVO.setScore(JudgeConstants.ERROR_SCORE);
            userQuestionResultVO.setPass(Constants.FALSE);
            userQuestionResultVO.setExeMessage(CodeRunStatus.NOT_ALL_PASSED.getMsg());
            return userQuestionResultVO;
        }
        List<UserExeResult> userExeResultList = new ArrayList<>();
        boolean passed = resultCompare(judgeSubmitDTO, exeOutputList, outputList, userExeResultList);
        return assembleUserQuestionResultVO(judgeSubmitDTO, sandBoxExecuteResult, userQuestionResultVO, userExeResultList, passed);
    }

    private UserQuestionResultVO assembleUserQuestionResultVO(JudgeSubmitDTO judgeSubmitDTO,
                                                              SandBoxExecuteResult sandBoxExecuteResult,
                                                              UserQuestionResultVO userQuestionResultVO,
                                                              List<UserExeResult> userExeResultList, boolean passed) {
        userQuestionResultVO.setUserExeResultList(userExeResultList);
        if (!passed) {
            userQuestionResultVO.setPass(Constants.FALSE);
            userQuestionResultVO.setScore(JudgeConstants.ERROR_SCORE);
            userQuestionResultVO.setExeMessage(CodeRunStatus.NOT_ALL_PASSED.getMsg());
            return userQuestionResultVO;
        }
        if (sandBoxExecuteResult.getUseMemory() > judgeSubmitDTO.getSpaceLimit()) {
            userQuestionResultVO.setPass(Constants.FALSE);
            userQuestionResultVO.setScore(JudgeConstants.ERROR_SCORE);
            userQuestionResultVO.setExeMessage(CodeRunStatus.OUT_OF_MEMORY.getMsg());
            return userQuestionResultVO;
        }
        if (sandBoxExecuteResult.getUseTime() > judgeSubmitDTO.getTimeLimit()) {
            userQuestionResultVO.setPass(Constants.FALSE);
            userQuestionResultVO.setScore(JudgeConstants.ERROR_SCORE);
            userQuestionResultVO.setExeMessage(CodeRunStatus.OUT_OF_TIME.getMsg());
            return userQuestionResultVO;
        }
        userQuestionResultVO.setPass(Constants.TRUE);
        int score = judgeSubmitDTO.getDifficulty() * JudgeConstants.DEFAULT_SCORE;
        userQuestionResultVO.setScore(score);
        return userQuestionResultVO;
    }

    private boolean resultCompare(JudgeSubmitDTO judgeSubmitDTO, List<String> exeOutputList,
                                  List<String> outputList, List<UserExeResult> userExeResultList) {
        boolean passed = true;
        for (int index = 0; index < outputList.size(); index++) {
            String output = outputList.get(index);
            String exeOutPut = exeOutputList.get(index);
            String input = judgeSubmitDTO.getInputList().get(index);
            UserExeResult userExeResult = new UserExeResult();
            userExeResult.setInput(input);
            userExeResult.setOutput(output);
            userExeResult.setExeOutput(exeOutPut);
            userExeResultList.add(userExeResult);
            if (!output.equals(exeOutPut)) {
                passed = false;
                log.info("输入：{}，期望输出：{}，实际输出：{} ", input, output, exeOutPut);
            }
        }
        return passed;
    }

    private void saveUserSubmit(JudgeSubmitDTO judgeSubmitDTO, UserQuestionResultVO userQuestionResultVO) {
        UserSubmit userSubmit = new UserSubmit();
        BeanUtil.copyProperties(userQuestionResultVO, userSubmit);
        userSubmit.setUserId(judgeSubmitDTO.getUserId());
        userSubmit.setQuestionId(judgeSubmitDTO.getQuestionId());
        userSubmit.setExamId(judgeSubmitDTO.getExamId());
        userSubmit.setProgramType(judgeSubmitDTO.getProgramType());
        userSubmit.setUserCode(judgeSubmitDTO.getUserCode());
        userSubmit.setCaseJudgeRes(JSON.toJSONString(userQuestionResultVO.getUserExeResultList()));
        userSubmit.setCreateBy(judgeSubmitDTO.getUserId());
        userSubmitMapper.delete(new LambdaQueryWrapper<UserSubmit>()
                .eq(UserSubmit::getUserId, judgeSubmitDTO.getUserId())
                .eq(UserSubmit::getQuestionId, judgeSubmitDTO.getQuestionId())
                .isNull(judgeSubmitDTO.getExamId() == null, UserSubmit::getExamId)
                .eq(judgeSubmitDTO.getExamId() != null, UserSubmit::getExamId, judgeSubmitDTO.getExamId()));
        userSubmitMapper.insert(userSubmit);
    }
}

