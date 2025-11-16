package com.nushungry.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyResetCodeRequest {
    private String email;
    private String code;
}
