package com.nushungry.adminservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BatchOperationRequest {
    private List<Long> userIds;
    private String operation;
}