package com.openroof.openroof.service;

import com.openroof.openroof.dto.preference.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.UserPreferenceMapper;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.preference.PreferenceOption;
import com.openroof.openroof.model.preference.UserPreference;
import com.openroof.openroof.model.preference.UserPreferenceRange;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio para gestionar las preferencias de búsqueda de los usuarios.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final PreferenceCategoryRepository preferenceCategoryRepository;
    private final PreferenceOptionRepository preferenceOptionRepository;
    private final UserRepository userRepository;
    private final UserPreferenceMapper userPreferenceMapper;

    // ─── Catálogo ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PreferenceCategoryDTO> getPreferenceOptions() {
        return preferenceCategoryRepository.findAllWithOptions()
                .stream()
                .map(userPreferenceMapper::toCategoryDTO)
                .toList();
    }

    // ─── Lectura ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserPreferenceResponseDTO getUserPreferences(Long userId) {
        checkOwnership(userId);
        validateUserExists(userId);

        return userPreferenceRepository.findByUserId(userId)
                .map(userPreferenceMapper::toResponseDTO)
                .orElseGet(() -> userPreferenceMapper.emptyResponseDTO(userId));
    }

    // ─── Upsert ────────────────────────────────────────────────────────────────

    public UserPreferenceResponseDTO saveOrUpdateUserPreferences(UserPreferenceRequestDTO dto) {
        checkOwnership(dto.userId());
        
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + dto.userId()));

        List<PreferenceOption> selectedOptions = resolveOptions(dto.selectedOptionIds());

        UserPreference pref = userPreferenceRepository.findByUserId(dto.userId())
                .orElseGet(() -> UserPreference.builder().user(user).build());

        Set<PreferenceOption> optionSet = new HashSet<>(selectedOptions);
        pref.setSelectedOptions(optionSet);

        pref.getRanges().clear();
        if (dto.ranges() != null && !dto.ranges().isEmpty()) {
            List<UserPreferenceRange> ranges = buildRanges(dto.ranges(), pref);
            pref.getRanges().addAll(ranges);
        }

        pref.setOnboardingCompleted(true);
        UserPreference saved = userPreferenceRepository.save(pref);

        user.setOnboardingCompleted(true);
        userRepository.save(user);

        return userPreferenceMapper.toResponseDTO(saved);
    }

    // ─── Eliminación ───────────────────────────────────────────────────────────

    public void deleteUserPreferences(Long userId) {
        checkOwnership(userId);
        validateUserExists(userId);

        userPreferenceRepository.findByUserId(userId)
                .ifPresent(userPreferenceRepository::delete);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        user.setOnboardingCompleted(false);
        userRepository.save(user);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void checkOwnership(Long userId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("No autenticado");
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof User currentUser)) {
            throw new ForbiddenException("No se pudo determinar el usuario actual");
        }

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = currentUser.getId().equals(userId);

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("No tienes permiso para acceder a las preferencias de otro usuario");
        }
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    private List<PreferenceOption> resolveOptions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> distinctIds = ids.stream().distinct().toList();
        List<PreferenceOption> found = preferenceOptionRepository.findAllById(distinctIds);

        if (found.size() != distinctIds.size()) {
            Set<Long> foundIds = new HashSet<>();
            found.forEach(o -> foundIds.add(o.getId()));
            List<Long> missing = ids.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BadRequestException(
                    "Las siguientes opciones de preferencia no existen: " + missing);
        }
        return found;
    }

    private List<UserPreferenceRange> buildRanges(List<RangeDTO> rangeDTOs, UserPreference pref) {
        List<UserPreferenceRange> ranges = new ArrayList<>();
        for (RangeDTO dto : rangeDTOs) {
            UserPreferenceRange range = UserPreferenceRange.builder()
                    .userPreference(pref)
                    .fieldName(dto.fieldName())
                    .minValue(dto.minValue())
                    .maxValue(dto.maxValue())
                    .build();
            ranges.add(range);
        }
        return ranges;
    }
}
