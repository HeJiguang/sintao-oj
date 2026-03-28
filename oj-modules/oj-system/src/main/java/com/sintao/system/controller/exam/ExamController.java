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
@Tag(name = "后台测试管理接口")
public class ExamController extends BaseController {

    @Autowired
    private IExamService examService;

    @GetMapping("/list")
    @Operation(summary = "测试列表", description = "分页查询测试列表，支持按标题等条件筛选")
    @ApiResponse(responseCode = "200", description = "成功返回分页数据")
    public TableDataInfo list(ExamQueryDTO examQueryDTO) {
        return getTableDataInfo(examService.list(examQueryDTO));
    }

    @PostMapping("/add")
    @Operation(summary = "新增测试", description = "创建新的阶段测试，并返回测试ID")
    @ApiResponse(responseCode = "1000", description = "新增成功，返回测试ID")
    @ApiResponse(responseCode = "2000", description = "服务繁忙，请稍后重试")
    public R<String> add(@RequestBody ExamAddDTO examAddDTO) {
        return R.ok(examService.add(examAddDTO));
    }

    @PostMapping("/question/add")
    @Operation(summary = "测试添加题目", description = "为指定测试关联题目")
    @ApiResponse(responseCode = "1000", description = "添加成功")
    @ApiResponse(responseCode = "2000", description = "题目不存在、测试已发布或服务异常")
    public R<Void> questionAdd(@RequestBody ExamQuestAddDTO examQuestAddDTO) {
        return toR(examService.questionAdd(examQuestAddDTO));
    }

    @DeleteMapping("/question/delete")
    @Operation(summary = "测试移除题目", description = "从指定测试中移除题目")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "测试ID", required = true)
    @Parameter(name = "questionId", in = ParameterIn.QUERY, description = "题目ID", required = true)
    @ApiResponse(responseCode = "1000", description = "移除成功")
    @ApiResponse(responseCode = "2000", description = "测试已发布或服务异常")
    public R<Void> questionDelete(
            @Parameter(description = "测试ID") Long examId,
            @Parameter(description = "题目ID") Long questionId) {
        return toR(examService.questionDelete(examId, questionId));
    }

    @GetMapping("/detail")
    @Operation(summary = "测试详情", description = "根据测试ID获取测试详情和关联题目")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "测试ID", required = true)
    @ApiResponse(responseCode = "1000", description = "成功返回测试详情")
    @ApiResponse(responseCode = "2000", description = "测试不存在或服务异常")
    public R<ExamDetailVO> detail(@Parameter(description = "测试ID") Long examId) {
        return R.ok(examService.detail(examId));
    }

    @PutMapping("/edit")
    @Operation(summary = "编辑测试", description = "更新测试信息，未发布测试可编辑")
    @ApiResponse(responseCode = "1000", description = "编辑成功")
    @ApiResponse(responseCode = "2000", description = "测试已发布或服务异常")
    public R<Void> edit(@RequestBody ExamEditDTO examEditDTO) {
        return toR(examService.edit(examEditDTO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除测试", description = "删除测试，未发布测试可删除")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "测试ID", required = true)
    @ApiResponse(responseCode = "1000", description = "删除成功")
    @ApiResponse(responseCode = "2000", description = "测试已发布或服务异常")
    public R<Void> delete(@Parameter(description = "测试ID") Long examId) {
        return toR(examService.delete(examId));
    }

    @PutMapping("/publish")
    @Operation(summary = "发布测试", description = "发布测试，发布后会出现在用户侧测试列表并写入缓存")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "测试ID", required = true)
    @ApiResponse(responseCode = "1000", description = "发布成功")
    @ApiResponse(responseCode = "2000", description = "测试已结束、没有题目或服务异常")
    public R<Void> publish(@Parameter(description = "测试ID") Long examId) {
        return toR(examService.publish(examId));
    }

    @PutMapping("/cancelPublish")
    @Operation(summary = "取消发布", description = "取消测试发布，并从缓存中移除")
    @Parameter(name = "examId", in = ParameterIn.QUERY, description = "测试ID", required = true)
    @ApiResponse(responseCode = "1000", description = "取消成功")
    @ApiResponse(responseCode = "2000", description = "测试已开始或服务异常")
    public R<Void> cancelPublish(@Parameter(description = "测试ID") Long examId) {
        return toR(examService.cancelPublish(examId));
    }
}
