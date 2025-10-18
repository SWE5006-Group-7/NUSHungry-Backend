package com.nushungry.preference.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.nushungry.preference.service.SearchHistoryService;
import java.util.List;

@RestController
@RequestMapping("/preference/search-history")
public class SearchHistoryController {
    @Autowired
    private SearchHistoryService searchHistoryService;

    /**
     * Add a search history record for a user.
     */
    @PostMapping("/add")
    public String addHistory(@RequestParam Long userId, @RequestParam String keyword) {
        searchHistoryService.addHistory(userId, keyword);
        return "success";
    }

    /**
     * Get the list of search history keywords for a user.
     */
    @GetMapping("/list")
    public List<String> listHistory(@RequestParam Long userId) {
        return searchHistoryService.listHistory(userId);
    }

    /**
     * Batch remove search history keywords for a user.
     */
    @PostMapping("/batchRemove")
    public String batchRemove(@RequestParam Long userId, @RequestBody List<String> keywords) {
        searchHistoryService.batchRemove(userId, keywords);
        return "success";
    }

    /**
     * Clear all search history for a user.
     */
    @DeleteMapping("/clear")
    public String clearHistory(@RequestParam Long userId) {
        searchHistoryService.clearHistory(userId);
        return "success";
    }
}
