package com.openroof.openroof.service;

import com.openroof.openroof.dto.search.SearchPreferenceRequest;
import com.openroof.openroof.dto.search.SearchPreferenceResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.search.SearchPreference;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.SearchPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchPreferenceServiceTest {

    @Mock
    private SearchPreferenceRepository repository;

    private SearchPreferenceService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        service = new SearchPreferenceService(repository);
        testUser = User.builder().email("test@test.com").build();
        testUser.setId(1L);
    }

    @Test
    void createSearchPreference_success() {
        SearchPreferenceRequest request = new SearchPreferenceRequest("My Search", Map.of("city", "Madrid"));
        when(repository.countByUserId(testUser.getId())).thenReturn(0L);
        when(repository.save(any(SearchPreference.class))).thenAnswer(invocation -> {
            SearchPreference pref = invocation.getArgument(0);
            pref.setId(1L);
            pref.setCreatedAt(LocalDateTime.now());
            pref.setUpdatedAt(LocalDateTime.now());
            return pref;
        });

        SearchPreferenceResponse result = service.createSearchPreference(request, testUser);

        assertNotNull(result);
        assertEquals("My Search", result.name());
        assertEquals("Madrid", result.filters().get("city"));
        verify(repository).save(any(SearchPreference.class));
    }

    @Test
    void createSearchPreference_exceedsMax10_throwsBadRequest() {
        SearchPreferenceRequest request = new SearchPreferenceRequest("Extra Search", Map.of());
        when(repository.countByUserId(testUser.getId())).thenReturn(10L);

        assertThrows(BadRequestException.class, () ->
                service.createSearchPreference(request, testUser));
    }

    @Test
    void updateName_success() {
        SearchPreference existingPref = SearchPreference.builder()
                .user(testUser)
                .name("Old Name")
                .filters(Map.of())
                .build();
        existingPref.setId(1L);
        when(repository.findByIdAndUserId(1L, testUser.getId())).thenReturn(Optional.of(existingPref));
        when(repository.save(any(SearchPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SearchPreferenceResponse result = service.updateName(1L, "New Name", testUser);

        assertEquals("New Name", result.name());
    }

    @Test
    void updateName_notFound_throwsResourceNotFound() {
        SearchPreference existingPref = SearchPreference.builder()
                .user(testUser)
                .name("Old Name")
                .filters(Map.of())
                .build();
        existingPref.setId(1L);
        when(repository.findByIdAndUserId(1L, testUser.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.updateName(1L, "New Name", testUser));
    }

    @Test
    void deleteSearchPreference_success() {
        SearchPreference existingPref = SearchPreference.builder()
                .user(testUser)
                .name("Test")
                .filters(Map.of())
                .build();
        existingPref.setId(1L);
        when(repository.findByIdAndUserId(1L, testUser.getId())).thenReturn(Optional.of(existingPref));

        service.deleteSearchPreference(1L, testUser);

        verify(repository).delete(existingPref);
    }

    @Test
    void deleteSearchPreference_notFound_throwsResourceNotFound() {
        SearchPreference existingPref = SearchPreference.builder()
                .user(testUser)
                .name("Test")
                .filters(Map.of())
                .build();
        existingPref.setId(1L);
        when(repository.findByIdAndUserId(1L, testUser.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.deleteSearchPreference(1L, testUser));
    }

    @Test
    void getUserSearchPreferences_returnsOnlyUserPreferences() {
        Pageable pageable = PageRequest.of(0, 10);
        SearchPreference pref = SearchPreference.builder()
                .user(testUser)
                .name("Test")
                .filters(Map.of())
                .build();
        pref.setId(1L);
        Page<SearchPreference> page = new PageImpl<>(List.of(pref));
        when(repository.findByUserId(testUser.getId(), pageable)).thenReturn(page);

        Page<SearchPreferenceResponse> result = service.getUserSearchPreferences(testUser, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Test", result.getContent().get(0).name());
    }
}