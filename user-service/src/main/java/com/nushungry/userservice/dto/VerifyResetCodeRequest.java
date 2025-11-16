package com.nushungry.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 验证重置验证码请求
 */
@Data
public class VerifyResetCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 10, message = "验证码长度不正确")
    private String code;
}
