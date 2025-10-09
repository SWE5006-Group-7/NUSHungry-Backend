package com.nushungry.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/**
 * 更新评价请求 DTO
 */
@Data
public class UpdateReviewRequest {

    @NotNull(message = "评分不能为空")
    @DecimalMin(value = "1.0", message = "评分必须在1到5之间")
    @DecimalMax(value = "5.0", message = "评分必须在1到5之间")
    private Double rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(min = 10, max = 1000, message = "评价内容长度必须在10到1000个字符之间")
    private String comment;

    @Size(max = 9, message = "最多只能上传9张图片")
    private List<String> imageUrls;
}
