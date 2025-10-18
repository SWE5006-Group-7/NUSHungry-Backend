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
        Favorite f2 = new Favorite();
        f2.setStallId(3L);
        f2.setCreatedAt(200L);
        List<Favorite> favs = List.of(f1, f2);
        when(favoriteRepository.findByUserId(1L)).thenReturn(new ArrayList<>(favs));
        List<Long> result = favoriteService.sortedFavorites(1L);
        assertEquals(List.of(3L, 2L), result);
    }
}

