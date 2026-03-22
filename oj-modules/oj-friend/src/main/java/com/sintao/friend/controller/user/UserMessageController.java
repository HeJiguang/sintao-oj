package com.sintao.friend.controller.user;

import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.PageQueryDTO;
import com.sintao.common.core.domain.TableDataInfo;
import com.sintao.friend.service.user.IUserMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/message")
@Tag(name = "C 端消息接口", description = "用户消息列表，如竞赛排名通知等")
public class UserMessageController extends BaseController {

    @Autowired
    private IUserMessageService userMessageService;

    @GetMapping("/list")
    @Operation(summary = "消息列表", description = "分页查询当前用户收到的系统消息")
    @ApiResponse(responseCode = "200", description = "成功返回分页数据")
    public TableDataInfo list(PageQueryDTO dto) {
        return userMessageService.list(dto);
    }
}

