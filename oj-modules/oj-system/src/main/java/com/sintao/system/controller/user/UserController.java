package com.sintao.system.controller.user;

import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.TableDataInfo;
import com.sintao.system.domain.user.dto.UserDTO;
import com.sintao.system.domain.user.dto.UserQueryDTO;
import com.sintao.system.service.user.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Tag(name = "C 端用户管理接口")
public class UserController extends BaseController {

    @Autowired
    private IUserService userService;

    @GetMapping("/list")
    @Operation(summary = "用户列表", description = "分页查询 C 端用户列表，支持按昵称、手机号等条件筛选")
    @ApiResponse(responseCode = "200", description = "成功返回分页数据")
    public TableDataInfo list(UserQueryDTO userQueryDTO) {
        return getTableDataInfo(userService.list(userQueryDTO));
    }

    @PutMapping("/updateStatus")
    @Operation(summary = "更新用户状态", description = "拉黑/解禁用户：更新用户状态，拉黑后限制用户操作，解禁后放开限制")
    @ApiResponse(responseCode = "1000", description = "更新成功")
    @ApiResponse(responseCode = "2000", description = "用户不存在或服务异常")
    public R<Void> updateStatus(@RequestBody UserDTO userDTO) {
        return toR(userService.updateStatus(userDTO));
    }
}

