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

    @NotNull(message = "nickName must not be null")
    private String nickName;

    @NotEmpty(message = "userAccount must not be empty")
    private String userAccount;

    @NotBlank(message = "password must not be blank")
    @Size(min = 5, max = 10, message = "password length must be between 5 and 10")
    private String password;

    @Min(value = 0, message = "age must be >= 0")
    @Max(value = 60, message = "age must be <= 60")
    private int age;

    @Email(message = "email format is invalid")
    private String email;

    @Pattern(
            regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$",
            message = "phone format is invalid"
    )
    private String phone;

    @Past(message = "startDate must be in the past")
    private LocalDate startDate;

    @Future(message = "endDate must be in the future")
    private LocalDate endDate;
}
