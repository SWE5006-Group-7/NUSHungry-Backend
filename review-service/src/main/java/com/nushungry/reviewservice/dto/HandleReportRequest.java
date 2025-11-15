package com.nushungry.reviewservice.dto;

import com.nushungry.reviewservice.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandleReportRequest {

    @NotNull(message = "Status is required")
    private ReportStatus status;

    private String handleNote;
}
