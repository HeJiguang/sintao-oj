package com.sintao.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sintao.common.core.domain.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@TableName("tb_sys_user")
@Getter
@Setter
@ToString
public class SysUser extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long userId; // 主键 不再使用auto_increment维护主键

    private String userAccount;

    private String password;
}
