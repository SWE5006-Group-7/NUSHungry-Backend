package com.nushungry.dto;

import com.nushungry.model.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理举报请求DTO（管理员使用）
 */
@Data
public class HandleReportRequest {

    @NotNull(message = "处理状态不能为空")
    private ReportStatus status;

    @Size(max = 500, message = "处理备注不能超过500个字符")
    private String handleNote;
}
