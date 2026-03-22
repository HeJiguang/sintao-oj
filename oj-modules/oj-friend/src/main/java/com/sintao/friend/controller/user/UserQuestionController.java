package com.sintao.friend.controller.user;

import com.sintao.api.domain.vo.UserQuestionResultVO;
import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import com.sintao.friend.domain.user.dto.UserSubmitDTO;
import com.sintao.friend.service.user.IUserQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/question")
@Tag(name = "C 端答题接口", description = "用户提交代码、查询判题结果等接口")
public class UserQuestionController extends BaseController {

    @Autowired
    private IUserQuestionService userQuestionService;

    @PostMapping("/submit")
    @Operation(summary = "同步提交代码", description = "提交代码并同步等待判题结果，适用于需要立即返回结果的场景")
    @ApiResponse(responseCode = "200", description = "判题完成，返回判题结果")
    @ApiResponse(responseCode = "2000", description = "不支持的语言类型或判题失败")
    public R<UserQuestionResultVO> submit(@RequestBody UserSubmitDTO submitDTO) {
        return userQuestionService.submit(submitDTO);
    }

    @PostMapping("/rabbit/submit")
    @Operation(summary = "异步提交代码", description = "通过 RabbitMQ 异步提交代码，立即返回，需轮询 exe/result 获取判题结果")
    @ApiResponse(responseCode = "200", description = "提交成功")
    @ApiResponse(responseCode = "2000", description = "不支持的语言类型或消息发送失败")
    public R<Void> rabbitSubmit(@RequestBody UserSubmitDTO submitDTO) {
        return toR(userQuestionService.rabbitSubmit(submitDTO));
    }

    @GetMapping("/exe/result")
    @Operation(summary = "查询判题结果", description = "轮询获取异步提交的判题结果，currentTime 为提交时的时间戳")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "竞赛ID，练习题为空")
    @Parameter(name = "questionId", in = ParameterIn.QUERY, description = "题目ID", required = true)
    @Parameter(name = "currentTime", in = ParameterIn.QUERY, description = "提交时间，用于筛选该次提交")
    @ApiResponse(responseCode = "200", description = "成功返回判题结果，pass 为判题中时表示尚未完成")
    public R<UserQuestionResultVO> exeResult(
            @Parameter(description = "竞赛ID") Long examId,
            @Parameter(description = "题目ID") Long questionId,
            @Parameter(description = "提交时间") String currentTime) {
        return R.ok(userQuestionService.exeResult(examId, questionId, currentTime));
    }
}

