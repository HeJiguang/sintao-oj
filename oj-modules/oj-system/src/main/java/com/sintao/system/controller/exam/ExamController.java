package com.sintao.system.controller.exam;

import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.TableDataInfo;
import com.sintao.system.domain.exam.dto.ExamAddDTO;
import com.sintao.system.domain.exam.dto.ExamEditDTO;
import com.sintao.system.domain.exam.dto.ExamQueryDTO;
import com.sintao.system.domain.exam.dto.ExamQuestAddDTO;
import com.sintao.system.domain.exam.vo.ExamDetailVO;
import com.sintao.system.service.exam.IExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exam")
@Tag(name = "竞赛管理接口")
public class ExamController extends BaseController {

    @Autowired
    private IExamService examService;

    @GetMapping("/list")
    @Operation(summary = "竞赛列表", description = "分页查询竞赛列表，支持按标题等条件筛选")
    @ApiResponse(responseCode = "200", description = "成功返回分页数据")
    public TableDataInfo list(ExamQueryDTO examQueryDTO) {
        return getTableDataInfo(examService.list(examQueryDTO));
    }

    @PostMapping("/add")
    @Operation(summary = "新增竞赛", description = "创建新竞赛，返回竞赛ID")
    @ApiResponse(responseCode = "1000", description = "新增成功，返回竞赛ID")
    @ApiResponse(responseCode = "2000", description = "服务繁忙，请稍后重试")
    public R<String> add(@RequestBody ExamAddDTO examAddDTO) {
        return R.ok(examService.add(examAddDTO));
    }

    @PostMapping("/question/add")
    @Operation(summary = "竞赛添加题目", description = "为指定竞赛关联题目")
    @ApiResponse(responseCode = "1000", description = "添加成功")
    @ApiResponse(responseCode = "2000", description = "题目不存在、竞赛已发布或服务异常")
    public R<Void> questionAdd(@RequestBody ExamQuestAddDTO examQuestAddDTO) {
        return toR(examService.questionAdd(examQuestAddDTO));
    }

    @DeleteMapping("/question/delete")
    @Operation(summary = "竞赛移除题目", description = "从竞赛中移除指定题目")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "竞赛ID", required = true)
    @Parameter(name = "questionId", in = ParameterIn.QUERY, description = "题目ID", required = true)
    @ApiResponse(responseCode = "1000", description = "移除成功")
    @ApiResponse(responseCode = "2000", description = "竞赛已发布或服务异常")
    public R<Void> questionDelete(
            @Parameter(description = "竞赛ID") Long examId,
            @Parameter(description = "题目ID") Long questionId) {
        return toR(examService.questionDelete(examId, questionId));
    }

    @GetMapping("/detail")
    @Operation(summary = "竞赛详情", description = "根据竞赛 ID 获取竞赛详情及关联题目")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "竞赛ID", required = true)
    @ApiResponse(responseCode = "1000", description = "成功返回竞赛详情")
    @ApiResponse(responseCode = "2000", description = "竞赛不存在或服务异常")
    public R<ExamDetailVO> detail(@Parameter(description = "竞赛ID") Long examId) {
        return R.ok(examService.detail(examId));
    }

    @PutMapping("/edit")
    @Operation(summary = "编辑竞赛", description = "更新竞赛信息，仅未发布的竞赛可编辑")
    @ApiResponse(responseCode = "1000", description = "编辑成功")
    @ApiResponse(responseCode = "2000", description = "竞赛已发布或服务异常")
    public R<Void> edit(@RequestBody ExamEditDTO examEditDTO) {
        return toR(examService.edit(examEditDTO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除竞赛", description = "删除竞赛，仅未发布的竞赛可删除")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "竞赛ID", required = true)
    @ApiResponse(responseCode = "1000", description = "删除成功")
    @ApiResponse(responseCode = "2000", description = "竞赛已发布或服务异常")
    public R<Void> delete(@Parameter(description = "竞赛ID") Long examId) {
        return toR(examService.delete(examId));
    }

    @PutMapping("/publish")
    @Operation(summary = "发布竞赛", description = "发布竞赛，发布后竞赛将出现在 C 端未完赛列表并写入 Redis")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "竞赛ID", required = true)
    @ApiResponse(responseCode = "1000", description = "发布成功")
    @ApiResponse(responseCode = "2000", description = "竞赛已结束、无题目或服务异常")
    public R<Void> publish(@Parameter(description = "竞赛ID") Long examId) {
        return toR(examService.publish(examId));
    }

    @PutMapping("/cancelPublish")
    @Operation(summary = "取消发布", description = "取消竞赛发布，并从 Redis 中移除")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "竞赛ID", required = true)
    @ApiResponse(responseCode = "1000", description = "取消成功")
    @ApiResponse(responseCode = "2000", description = "竞赛已开始或服务异常")
    public R<Void> cancelPublish(@Parameter(description = "竞赛ID") Long examId) {
        return toR(examService.cancelPublish(examId));
    }
}

