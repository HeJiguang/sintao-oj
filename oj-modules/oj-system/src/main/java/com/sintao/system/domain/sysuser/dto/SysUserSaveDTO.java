package com.sintao.system.domain.sysuser.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

//参数合法性判�?public class SysUserSaveDTO {

    @Schema(description = "用户账号")
    private String userAccount;

    @Schema(description = "用户密码")
    private String password;
}

