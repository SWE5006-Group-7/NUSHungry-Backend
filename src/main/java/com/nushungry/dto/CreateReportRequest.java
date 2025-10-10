package com.nushungry.dto;

import com.nushungry.model.ReviewReport;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建举报请求DTO
 */
@Data
public class CreateReportRequest {

    @NotNull(message = "举报原因不能为空")
    private ReviewReport.ReportReason reason;

    private String description; // 详细描述（可选）
}
