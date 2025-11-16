package com.nushungry.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建评价请求 DTO
 */
@Data
public class CreateReviewRequest {

    @NotNull(message = "摊位ID不能为空")
    private Long stallId;

    @NotNull(message = "评分不能为空")
    @DecimalMin(value = "1.0", message = "评分必须在1到5之间")
    @DecimalMax(value = "5.0", message = "评分必须在1到5之间")
    private Double rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(min = 10, max = 1000, message = "评价内容长度必须在10到1000个字符之间")
    private String comment;

    @Size(max = 9, message = "最多只能上传9张图片")
    private List<String> imageUrls = new ArrayList<>();

    @DecimalMin(value = "0.01", message = "总花费必须大于0")
    private Double totalCost; // 总花费（可选）

    @Min(value = 1, message = "用餐人数必须至少为1")
    private Integer numberOfPeople; // 用餐人数（可选）
}
