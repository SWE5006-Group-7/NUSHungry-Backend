package com.nushungry.dto;

import com.nushungry.model.ReviewReport;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 处理举报请求DTO（管理员使用）
 */
@Data
public class HandleReportRequest {

    @NotNull(message = "处理状态不能为空")
    private ReviewReport.ReportStatus status; // ACCEPTED 或 REJECTED

    private String handlerNote; // 处理说明（可选）

    private Boolean deleteReview; // 是否删除评价（仅在ACCEPTED时有效）
}
