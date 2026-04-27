package com.openroof.openroof.service;

import com.openroof.openroof.dto.search.SearchPreferenceRequest;
import com.openroof.openroof.dto.search.SearchPreferenceResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.search.SearchPreference;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.SearchPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SearchPreferenceService {

    private static final int MAX_PREFERENCES = 10;

    private final SearchPreferenceRepository repository;

    public SearchPreferenceResponse createSearchPreference(SearchPreferenceRequest request, User user) {
        long count = repository.countByUserId(user.getId());
        if (count >= MAX_PREFERENCES) {
            throw new BadRequestException("Maximum " + MAX_PREFERENCES + " saved searches allowed");
        }

        SearchPreference saved = repository.save(SearchPreference.builder()
                .user(user)
                .name(request.name())
                .filters(request.filters())
                .build());

        return toResponse(saved);
    }

    public SearchPreferenceResponse updateName(Long id, String name, User user) {
        SearchPreference existing = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Search preference not found"));

        existing.setName(name);
        return toResponse(repository.save(existing));
    }

    public void deleteSearchPreference(Long id, User user) {
        SearchPreference existing = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Search preference not found"));

        repository.delete(existing);
    }

    @Transactional(readOnly = true)
    public Page<SearchPreferenceResponse> getUserSearchPreferences(User user, Pageable pageable) {
        return repository.findByUserId(user.getId(), pageable)
                .map(this::toResponse);
    }

    private SearchPreferenceResponse toResponse(SearchPreference entity) {
        return new SearchPreferenceResponse(
                entity.getId(),
                entity.getName(),
                entity.getFilters(),
                entity.getNotificationsEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}