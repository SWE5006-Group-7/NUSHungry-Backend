package com.nushungry.reviewservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reviews")
@CompoundIndex(name = "stall_created_idx", def = "{'stallId': 1, 'createdAt': -1}")
@CompoundIndex(name = "stall_likes_idx", def = "{'stallId': 1, 'likesCount': -1}")
@CompoundIndex(name = "user_created_idx", def = "{'userId': 1, 'createdAt': -1}")
public class ReviewDocument {

    @Id
    private String id;

    @Indexed
    private Long stallId;

    private String stallName;

    @Indexed
    private String userId;

    private String username;

    private String userAvatarUrl;

    @Indexed
    private Integer rating;

    private String comment;

    private List<String> imageUrls;

    private Double totalCost;

    private Integer numberOfPeople;

    @Indexed
    @Builder.Default
    private Integer likesCount = 0;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
