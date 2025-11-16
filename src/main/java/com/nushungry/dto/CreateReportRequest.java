package com.nushungry.dto;

import com.nushungry.model.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建举报请求DTO
 */
@Data
public class CreateReportRequest {

    @NotNull(message = "举报原因不能为空")
    private ReportReason reason;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
}
