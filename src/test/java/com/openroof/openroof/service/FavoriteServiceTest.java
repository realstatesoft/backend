package com.openroof.openroof.service;

import com.openroof.openroof.dto.favorite.FavoriteActionResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.interaction.Favorite;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.FavoriteRepository;
import com.openroof.openroof.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private PropertyMapper propertyMapper;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, propertyRepository, propertyMapper);
    }

    private User user(long id) {
        User u = User.builder()
                .email("u@test.com")
                .passwordHash("x")
                .name("User")
                .role(UserRole.USER)
                .build();
        u.setId(id);
        return u;
    }

    private Property property(long id, Integer favoriteCount, LocalDateTime trashedAt) {
        User owner = user(1L);
        Property p = Property.builder()
                .title("Casa test")
                .propertyType(PropertyType.HOUSE)
                .address("Calle 1")
                .price(BigDecimal.ONE)
                .owner(owner)
                .rooms(List.of())
                .media(List.of())
                .exteriorFeatures(List.of())
                .build();
        p.setId(id);
        p.setFavoriteCount(favoriteCount);
        p.setTrashedAt(trashedAt);
        return p;
    }

    @Test
    void addFavorite_propertyNotFound_throwsResourceNotFoundException() {
        when(propertyRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> favoriteService.addFavorite(99L, user(5L)));

        verifyNoInteractions(favoriteRepository);
    }

    @Test
    void addFavorite_trashedProperty_throwsBadRequestException() {
        Property p = property(1L, 0, LocalDateTime.now());
        when(propertyRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p));

        assertThrows(BadRequestException.class,
                () -> favoriteService.addFavorite(1L, user(5L)));
    }

    @Test
    void addFavorite_alreadyFavorited_returnsSameCountWithoutSaving() {
        Property p = property(1L, 3, null);
        when(propertyRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        when(favoriteRepository.existsByUser_IdAndProperty_Id(5L, 1L)).thenReturn(true);

        FavoriteActionResponse r = favoriteService.addFavorite(1L, user(5L));

        assertTrue(r.favorited());
        assertEquals(1L, r.propertyId());
        assertEquals(3, r.favoriteCount());
        verify(favoriteRepository, never()).save(any());
        verify(propertyRepository, never()).save(any());
    }

    @Test
    void addFavorite_newFavorite_incrementsCountAndPersists() {
        Property p = property(1L, 2, null);
        when(propertyRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        when(favoriteRepository.existsByUser_IdAndProperty_Id(5L, 1L)).thenReturn(false);

        FavoriteActionResponse r = favoriteService.addFavorite(1L, user(5L));

        assertTrue(r.favorited());
        assertEquals(3, r.favoriteCount());

        ArgumentCaptor<Favorite> favCap = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository).save(favCap.capture());
        assertEquals(5L, favCap.getValue().getUser().getId());
        assertEquals(1L, favCap.getValue().getProperty().getId());

        ArgumentCaptor<Property> propCap = ArgumentCaptor.forClass(Property.class);
        verify(propertyRepository).save(propCap.capture());
        assertEquals(3, propCap.getValue().getFavoriteCount());
    }

    @Test
    void addFavorite_nullFavoriteCount_treatedAsZero() {
        Property p = property(1L, null, null);
        when(propertyRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        when(favoriteRepository.existsByUser_IdAndProperty_Id(5L, 1L)).thenReturn(false);

        FavoriteActionResponse r = favoriteService.addFavorite(1L, user(5L));

        assertEquals(1, r.favoriteCount());
        verify(propertyRepository).save(any());
    }
}
