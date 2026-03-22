package com.sintao.system.test.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ValidationDTO {

    @NotNull(message = "昵称不能为null")
    private String nickName;

    @NotEmpty(message = "用户账号不能为空")
    private String userAccount;

    @NotBlank(message = "用户密码不能为空，并且不能只包含空格")
    @Size(min = 5, max = 10, message = "密码长度不能少于5位，不能大于10�?)
    private String password;

    @Min(value = 0, message = "年龄不能小于0�?)
    @Max(value = 60, message = "年龄不能大于60�?)
    private int age;

    @Email(message = "必须符合邮箱格式")
    private String email;

    @Pattern(regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$", message = "手机号码格式不正�?)
    private String phone;

    @Past(message = "开始日期必须是过去的日�?)
    private LocalDate startDate;

    @Future(message = "结束日期必须是未来的日期")
    private LocalDate endDate;
}

