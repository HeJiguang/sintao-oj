package com.sintao.system.domain.sysuser.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SysUserSaveDTO {

    @Schema(description = "Admin user account")
    private String userAccount;

    @Schema(description = "Admin user password")
    private String password;
}
