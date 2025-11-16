package com.nushungry.preference.service;

import com.nushungry.preference.entity.Favorite;
import com.nushungry.preference.repository.FavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FavoriteServiceTest {
    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddFavorite() {
        when(favoriteRepository.existsByUserIdAndStallId(1L, 2L)).thenReturn(false);
        favoriteService.addFavorite(1L, 2L);
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
    }

    @Test
    void testRemoveFavorite() {
        favoriteService.removeFavorite(1L, 2L);
        verify(favoriteRepository, times(1)).deleteByUserIdAndStallId(1L, 2L);
    }

    @Test
    void testListFavorites() {
        List<Favorite> favs = new ArrayList<>();
        Favorite f = new Favorite();
        f.setStallId(2L);
        favs.add(f);
        when(favoriteRepository.findByUserId(1L)).thenReturn(favs);
        List<Long> result = favoriteService.listFavorites(1L);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0));
    }

    @Test
    void testBatchRemove() {
        List<Long> ids = List.of(2L, 3L);
        favoriteService.batchRemove(1L, ids);
        verify(favoriteRepository, times(1)).deleteByUserIdAndStallIdIn(1L, ids);
    }

    @Test
    void testSortedFavorites() {
        Favorite f1 = new Favorite();
        f1.setStallId(2L);
        f1.setCreatedAt(100L);
        f1.setSortOrder(0);
        Favorite f2 = new Favorite();
        f2.setStallId(3L);
        f2.setCreatedAt(200L);
        f2.setSortOrder(0);
        // 按 createdAt 降序排列: f2(200) > f1(100), 所以返回 [3L, 2L]
        List<Favorite> sortedFavs = List.of(f2, f1);
        when(favoriteRepository.findByUserIdOrderBySortOrderDescCreatedAtDesc(1L)).thenReturn(new ArrayList<>(sortedFavs));
        List<Long> result = favoriteService.sortedFavorites(1L);
        assertEquals(List.of(3L, 2L), result);
    }

    @Test
    void testAddFavorite_Duplicate_ShouldNotSave() {
        // 测试重复收藏防护：当收藏已存在时不应再次保存
        when(favoriteRepository.existsByUserIdAndStallId(1L, 2L)).thenReturn(true);
        favoriteService.addFavorite(1L, 2L);
        // 验证 save 方法从未被调用
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void testRemoveFavorite_NonExistent_ShouldNotThrow() {
        // 测试删除不存在的收藏：应正常执行不抛异常
        doNothing().when(favoriteRepository).deleteByUserIdAndStallId(1L, 999L);
        assertDoesNotThrow(() -> favoriteService.removeFavorite(1L, 999L));
        verify(favoriteRepository, times(1)).deleteByUserIdAndStallId(1L, 999L);
    }

    @Test
    void testIsFavorite_Exists() {
        when(favoriteRepository.existsByUserIdAndStallId(1L, 2L)).thenReturn(true);
        assertTrue(favoriteService.isFavorite(1L, 2L));
    }

    @Test
    void testIsFavorite_NotExists() {
        when(favoriteRepository.existsByUserIdAndStallId(1L, 999L)).thenReturn(false);
        assertFalse(favoriteService.isFavorite(1L, 999L));
    }

    @Test
    void testListFavorites_Empty() {
        when(favoriteRepository.findByUserId(1L)).thenReturn(new ArrayList<>());
        List<Long> result = favoriteService.listFavorites(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void testBatchRemove_EmptyList() {
        List<Long> emptyIds = new ArrayList<>();
        favoriteService.batchRemove(1L, emptyIds);
        verify(favoriteRepository, times(1)).deleteByUserIdAndStallIdIn(1L, emptyIds);
    }
}

