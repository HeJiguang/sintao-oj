package com.sintao.friend.controller.training;

import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import com.sintao.friend.domain.training.TrainingProfile;
import com.sintao.friend.domain.training.dto.TrainingGenerateDTO;
import com.sintao.friend.domain.training.dto.TrainingTaskFinishDTO;
import com.sintao.friend.domain.training.vo.TrainingCurrentVO;
import com.sintao.friend.service.training.ITrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/training")
@Tag(name = "C 端训练接口", description = "训练画像、训练计划生成、当前训练计划和任务完成接口")
public class TrainingController extends BaseController {

    @Autowired
    private ITrainingService trainingService;

    @GetMapping("/profile")
    @Operation(summary = "训练画像", description = "获取当前登录用户的训练画像")
    @ApiResponse(responseCode = "200", description = "成功返回训练画像")
    public R<TrainingProfile> profile() {
        return R.ok(trainingService.profile());
    }

    @GetMapping("/current")
    @Operation(summary = "当前训练计划", description = "获取当前登录用户的训练计划和任务列表")
    @ApiResponse(responseCode = "200", description = "成功返回训练计划")
    public R<TrainingCurrentVO> current() {
        return R.ok(trainingService.current());
    }

    @PostMapping("/generate")
    @Operation(summary = "生成训练计划", description = "根据用户历史提交和题目标签生成训练计划")
    @ApiResponse(responseCode = "200", description = "成功生成训练计划")
    public R<TrainingCurrentVO> generate(@RequestBody(required = false) TrainingGenerateDTO trainingGenerateDTO) {
        if (trainingGenerateDTO == null) {
            trainingGenerateDTO = new TrainingGenerateDTO();
        }
        return R.ok(trainingService.generate(trainingGenerateDTO));
    }

    @PostMapping("/task/finish")
    @Operation(summary = "完成训练任务", description = "标记训练任务已完成或已跳过")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public R<Void> finishTask(@RequestBody TrainingTaskFinishDTO trainingTaskFinishDTO) {
        return toR(trainingService.finishTask(trainingTaskFinishDTO));
    }
}
