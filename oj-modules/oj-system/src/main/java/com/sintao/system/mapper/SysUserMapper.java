package com.sintao.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sintao.system.domain.SysUser;
import jakarta.validation.constraints.Max;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

}
