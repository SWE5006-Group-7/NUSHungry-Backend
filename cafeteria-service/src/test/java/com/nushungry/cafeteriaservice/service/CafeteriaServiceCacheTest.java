package com.nushungry.cafeteriaservice.service;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.CafeteriaRepository;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * CafeteriaService 缓存功能测试
 * 
 * 测试 Redis 缓存是否正常工作
 * 测试缓存的存储和清除逻辑
 */
@SpringBootTest
@ActiveProfiles("test")
class CafeteriaServiceCacheTest {

    @Autowired
    private CafeteriaService cafeteriaService;

    @MockBean
    private CafeteriaRepository cafeteriaRepository;

    @MockBean
    private StallRepository stallRepository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // 清除所有缓存
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
        reset(cafeteriaRepository, stallRepository);
    }

    @Test
    void testFindAllCaching() {
        // Given
        Cafeteria cafeteria1 = new Cafeteria();
        cafeteria1.setId(1L);
        cafeteria1.setName("Fine Food");

        Cafeteria cafeteria2 = new Cafeteria();
        cafeteria2.setId(2L);
        cafeteria2.setName("The Deck");

        List<Cafeteria> cafeterias = Arrays.asList(cafeteria1, cafeteria2);
        when(cafeteriaRepository.findAll()).thenReturn(cafeterias);

        // When - 第一次调用
        List<Cafeteria> result1 = cafeteriaService.findAll();
        
        // When - 第二次调用（应该从缓存获取）
        List<Cafeteria> result2 = cafeteriaService.findAll();

        // Then
        assertThat(result1).hasSize(2);
        assertThat(result2).hasSize(2);
        
        // 验证 repository 只被调用一次（第二次从缓存获取）
        verify(cafeteriaRepository, times(1)).findAll();
    }

    @Test
    void testFindByIdCaching() {
        // Given
        Long cafeteriaId = 1L;
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setId(cafeteriaId);
        cafeteria.setName("Fine Food");

        when(cafeteriaRepository.findById(cafeteriaId)).thenReturn(Optional.of(cafeteria));

        // When - 第一次调用
        Cafeteria result1 = cafeteriaService.findById(cafeteriaId);

        // When - 第二次调用（应该从缓存获取）
        Cafeteria result2 = cafeteriaService.findById(cafeteriaId);

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.getName()).isEqualTo("Fine Food");

        // 验证 repository 只被调用一次
        verify(cafeteriaRepository, times(1)).findById(cafeteriaId);
    }

    @Test
    void testFindStallsByCafeteriaIdCaching() {
        // Given
        Long cafeteriaId = 1L;
        Stall stall1 = new Stall();
        stall1.setId(1L);
        stall1.setName("Stall 1");

        Stall stall2 = new Stall();
        stall2.setId(2L);
        stall2.setName("Stall 2");

        List<Stall> stalls = Arrays.asList(stall1, stall2);
        when(stallRepository.findByCafeteria_Id(cafeteriaId)).thenReturn(stalls);

        // When - 第一次调用
        List<Stall> result1 = cafeteriaService.findStallsByCafeteriaId(cafeteriaId);
        
        // When - 第二次调用（应该从缓存获取）
        List<Stall> result2 = cafeteriaService.findStallsByCafeteriaId(cafeteriaId);

        // Then
        assertThat(result1).hasSize(2);
        assertThat(result2).hasSize(2);
        
        // 验证 repository 只被调用一次
        verify(stallRepository, times(1)).findByCafeteria_Id(cafeteriaId);
    }

    @Test
    void testSaveClearsCaches() {
        // Given
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setId(1L);
        cafeteria.setName("Updated Cafeteria");

        List<Cafeteria> cafeterias = Arrays.asList(cafeteria);
        when(cafeteriaRepository.findAll()).thenReturn(cafeterias);
        when(cafeteriaRepository.save(any(Cafeteria.class))).thenReturn(cafeteria);
        when(cafeteriaRepository.findById(1L)).thenReturn(Optional.of(cafeteria));

        // When - 填充缓存
        cafeteriaService.findAll();
        cafeteriaService.findById(1L);
        
        // 验证已经被调用
        verify(cafeteriaRepository, times(1)).findAll();
        verify(cafeteriaRepository, times(1)).findById(1L);

        // When - 保存数据（应该清除缓存）
        cafeteriaService.save(cafeteria);

        // When - 再次查询（应该重新从数据库获取）
        cafeteriaService.findAll();
        cafeteriaService.findById(1L);

        // Then - 验证缓存被清除，重新从数据库查询
        verify(cafeteriaRepository, times(2)).findAll();
        verify(cafeteriaRepository, times(2)).findById(1L);
    }

    @Test
    void testDeleteClearsCaches() {
        // Given
        Long cafeteriaId = 1L;
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setId(cafeteriaId);
        cafeteria.setName("Test Cafeteria");

        List<Cafeteria> cafeterias = Arrays.asList(cafeteria);
        when(cafeteriaRepository.findAll()).thenReturn(cafeterias);
        when(cafeteriaRepository.findById(cafeteriaId)).thenReturn(Optional.of(cafeteria));

        // When - 填充缓存
        cafeteriaService.findAll();
        cafeteriaService.findById(cafeteriaId);

        verify(cafeteriaRepository, times(1)).findAll();
        verify(cafeteriaRepository, times(1)).findById(cafeteriaId);

        // When - 删除数据（应该清除缓存）
        cafeteriaService.deleteById(cafeteriaId);

        // When - 再次查询
        cafeteriaService.findAll();
        cafeteriaService.findById(cafeteriaId);

        // Then - 验证缓存被清除
        verify(cafeteriaRepository, times(2)).findAll();
        verify(cafeteriaRepository, times(2)).findById(cafeteriaId);
    }

    @Test
    void testCacheManagerAvailable() {
        // 验证 CacheManager 是否可用（在测试环境中可能不可用）
        if (cacheManager != null) {
            assertThat(cacheManager.getCacheNames()).contains("cafeterias", "cafeteria", "stalls");
        }
    }
}
