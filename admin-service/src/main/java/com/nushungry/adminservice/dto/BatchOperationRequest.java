package com.nushungry.adminservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchOperationRequest {
    private List<Long> userIds;
    private String operation;
}