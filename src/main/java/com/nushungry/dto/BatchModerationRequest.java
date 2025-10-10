package com.nushungry.dto;

import com.nushungry.model.ModerationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量审核请求DTO
 */
@Data
public class BatchModerationRequest {

    @NotEmpty(message = "评价ID列表不能为空")
    private List<Long> reviewIds;

    @NotNull(message = "审核动作不能为空")
    private ModerationStatus action; // APPROVED 或 REJECTED

    private String reason; // 审核原因(驳回时可选)
}
