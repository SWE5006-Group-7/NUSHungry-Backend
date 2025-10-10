package com.nushungry.dto;

import com.nushungry.model.ModerationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核请求DTO
 */
@Data
public class ModerationRequest {

    @NotNull(message = "审核动作不能为空")
    private ModerationStatus action; // APPROVED 或 REJECTED

    private String reason; // 审核原因(驳回时必填)
}
