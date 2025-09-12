package com.nushungry.service;

import com.nushungry.model.Review;
import com.nushungry.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public List<Review> findByStallId(Long stallId) {
        return reviewRepository.findByStallId(stallId);
    }

    public Review save(Review review) {
        return reviewRepository.save(review);
    }
}