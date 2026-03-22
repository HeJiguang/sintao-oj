package com.sintao.friend.controller.exam;

import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.TableDataInfo;
import com.sintao.friend.domain.exam.dto.ExamQueryDTO;
import com.sintao.friend.domain.exam.dto.ExamRankDTO;
import com.sintao.friend.service.exam.IExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exam")
@Tag(name = "C 端竞赛接口", description = "竞赛列表、排名、题目切换等接口，部分接口支持半登录访问")
public class ExamController extends BaseController {

    @Autowired
    private IExamService examService;

    @GetMapping("/semiLogin/list")
    @Operation(summary = "竞赛列表", description = "分页查询竞赛列表，从数据库查询")
    @ApiResponse(responseCode = "200", description = "成功返回分页数据")
    public TableDataInfo list(ExamQueryDTO examQueryDTO) {
        return getTableDataInfo(examService.list(examQueryDTO));
    }

    @GetMapping("/semiLogin/redis/list")
    @Operation(summary = "竞赛列表(Redis)", description = "分页查询竞赛列表，优先从 Redis 缓存获取，支持未完赛/历史竞赛")
    @ApiResponse(responseCode = "200", description = "成功返回分页数据")
    public TableDataInfo redisList(ExamQueryDTO examQueryDTO) {
        return examService.redisList(examQueryDTO);
    }

    @GetMapping("/rank/list")
    @Operation(summary = "竞赛排名", description = "分页查询指定竞赛的排名列表")
    @ApiResponse(responseCode = "200", description = "成功返回分页数据")
    public TableDataInfo rankList(ExamRankDTO examRankDTO) {
        return examService.rankList(examRankDTO);
    }

    @GetMapping("/getFirstQuestion")
    @Operation(summary = "获取竞赛第一题", description = "获取竞赛中题目的第一题 ID")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "竞赛ID", required = true)
    @ApiResponse(responseCode = "200", description = "成功返回第一题ID")
    @ApiResponse(responseCode = "2000", description = "竞赛不存在或无题目")
    public R<String> getFirstQuestion(@Parameter(description = "竞赛ID") Long examId) {
        return R.ok(examService.getFirstQuestion(examId));
    }

    @GetMapping("/preQuestion")
    @Operation(summary = "竞赛内上一题", description = "获取竞赛中当前题目的上一题 ID")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "竞赛ID", required = true)
    @Parameter(name = "questionId", in = ParameterIn.QUERY, description = "当前题目ID", required = true)
    @ApiResponse(responseCode = "200", description = "成功返回上一题ID")
    @ApiResponse(responseCode = "2000", description = "已是第一题")
    public R<String> preQuestion(
            @Parameter(description = "竞赛ID") Long examId,
            @Parameter(description = "当前题目ID") Long questionId) {
        return R.ok(examService.preQuestion(examId, questionId));
    }

    @GetMapping("/nextQuestion")
    @Operation(summary = "竞赛内下一题", description = "获取竞赛中当前题目的下一题 ID")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "竞赛ID", required = true)
    @Parameter(name = "questionId", in = ParameterIn.QUERY, description = "当前题目ID", required = true)
    @ApiResponse(responseCode = "200", description = "成功返回下一题ID")
    @ApiResponse(responseCode = "2000", description = "已是最后一题")
    public R<String> nextQuestion(
            @Parameter(description = "竞赛ID") Long examId,
            @Parameter(description = "当前题目ID") Long questionId) {
        return R.ok(examService.nextQuestion(examId, questionId));
    }
}

