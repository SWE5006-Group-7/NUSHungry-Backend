package com.nushungry.cafeteriaservice.service;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.CafeteriaRepository;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 食堂服务层
 * 
 * 缓存策略：
 * - findAll(): 缓存所有食堂列表（cafeterias）
 * - findById(): 缓存单个食堂（cafeteria::{id}）
 * - findStallsByCafeteriaId(): 缓存档口列表（stalls::{cafeteriaId}）
 * - save(): 清除相关缓存
 * - deleteById(): 清除相关缓存
 */
@Service
public class CafeteriaService {

    private final CafeteriaRepository cafeteriaRepository;
    private final StallRepository stallRepository;

    public CafeteriaService(CafeteriaRepository cafeteriaRepository, StallRepository stallRepository) {
        this.cafeteriaRepository = cafeteriaRepository;
        this.stallRepository = stallRepository;
    }

    /**
     * 获取所有食堂
     * 缓存所有食堂列表，TTL 10分钟
     */
    @Cacheable(value = "cafeterias", unless = "#result == null || #result.isEmpty()")
    public List<Cafeteria> findAll() {
        return cafeteriaRepository.findAll();
    }

    /**
     * 分页查询食堂（管理员功能）
     * 不使用缓存，因为管理员操作需要实时数据
     */
    public Page<Cafeteria> findAll(Pageable pageable) {
        return cafeteriaRepository.findAll(pageable);
    }

    /**
     * 根据 ID 获取食堂
     * 缓存单个食堂,TTL 10分钟
     * 注意: 返回Cafeteria而非Optional,以支持缓存
     */
    @Cacheable(value = "cafeteria", key = "#id", unless = "#result == null")
    public Cafeteria findById(Long id) {
        return cafeteriaRepository.findById(id).orElse(null);
    }

    /**
     * 根据 ID 获取食堂 Optional 版本 (内部使用)
     * 不缓存,直接查询数据库
     */
    public Optional<Cafeteria> findByIdOptional(Long id) {
        return cafeteriaRepository.findById(id);
    }

    /**
     * 获取指定食堂的所有档口
     * 缓存档口列表，TTL 5分钟
     */
    @Cacheable(value = "stalls", key = "#cafeteriaId", unless = "#result == null || #result.isEmpty()")
    public List<Stall> findStallsByCafeteriaId(Long cafeteriaId) {
        return stallRepository.findByCafeteria_Id(cafeteriaId);
    }

    /**
     * 保存或更新食堂
     * 清除相关缓存：
     * - 所有食堂列表缓存
     * - 该食堂的详情缓存
     * - 该食堂的档口列表缓存
     */
    @Caching(evict = {
        @CacheEvict(value = "cafeterias", allEntries = true),
        @CacheEvict(value = "cafeteria", key = "#cafeteria.id", condition = "#cafeteria.id != null"),
        @CacheEvict(value = "stalls", key = "#cafeteria.id", condition = "#cafeteria.id != null")
    })
    public Cafeteria save(Cafeteria cafeteria) {
        if (cafeteria == null) {
            throw new IllegalArgumentException("Cafeteria must not be null");
        }
        return cafeteriaRepository.save(cafeteria);
    }

    /**
     * 删除食堂
     * 清除相关缓存：
     * - 所有食堂列表缓存
     * - 该食堂的详情缓存
     * - 该食堂的档口列表缓存
     */
    @Caching(evict = {
        @CacheEvict(value = "cafeterias", allEntries = true),
        @CacheEvict(value = "cafeteria", key = "#id"),
        @CacheEvict(value = "stalls", key = "#id")
    })
    public void deleteById(Long id) {
        cafeteriaRepository.deleteById(id);
    }
}


