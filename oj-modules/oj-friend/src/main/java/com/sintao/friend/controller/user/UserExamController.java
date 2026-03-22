package com.sintao.friend.controller.user;

import com.sintao.common.core.constants.HttpConstants;
import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.TableDataInfo;
import com.sintao.friend.aspect.CheckUserStatus;
import com.sintao.friend.domain.exam.dto.ExamDTO;
import com.sintao.friend.domain.exam.dto.ExamQueryDTO;
import com.sintao.friend.service.user.IUserExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/exam")
@Tag(name = "C 端用户竞赛接口", description = "竞赛报名、我的竞赛列表等接口")
public class UserExamController extends BaseController {

    @Autowired
    private IUserExamService userExamService;

    @CheckUserStatus
    @PostMapping("/enter")
    @Operation(summary = "报名竞赛", description = "用户报名参加指定竞赛，需在竞赛开始前报名")
    @Parameter(name = HttpConstants.AUTHENTICATION, in = ParameterIn.HEADER, description = "登录凭证", required = true)
    @ApiResponse(responseCode = "200", description = "报名成功")
    @ApiResponse(responseCode = "2000", description = "竞赛不存在、已开始、已报名或用户已被禁用")
    public R<Void> enter(@RequestHeader(HttpConstants.AUTHENTICATION) String token, @RequestBody ExamDTO examDTO) {
        return toR(userExamService.enter(token, examDTO.getExamId()));
    }

    @GetMapping("/list")
    @Operation(summary = "我的竞赛列表", description = "分页查询当前用户已报名的竞赛列表")
    @ApiResponse(responseCode = "200", description = "成功返回分页数据")
    public TableDataInfo list(ExamQueryDTO examQueryDTO) {
        return userExamService.list(examQueryDTO);
    }
}

