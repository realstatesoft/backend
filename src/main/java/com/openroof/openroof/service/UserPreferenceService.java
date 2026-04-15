package com.openroof.openroof.service;

import com.openroof.openroof.dto.preference.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.UserPreferenceMapper;
import com.openroof.openroof.model.preference.PreferenceOption;
import com.openroof.openroof.model.preference.UserPreference;
import com.openroof.openroof.model.preference.UserPreferenceRange;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;
import lombok.RequiredArgsConstructor;
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

    /**
     * Retorna todas las categorías con sus opciones ordenadas (para poblar el wizard del frontend).
     */
    @Transactional(readOnly = true)
    public List<PreferenceCategoryDTO> getPreferenceOptions() {
        return preferenceCategoryRepository.findAllWithOptions()
                .stream()
                .map(userPreferenceMapper::toCategoryDTO)
                .toList();
    }

    // ─── Lectura ───────────────────────────────────────────────────────────────

    /**
     * Retorna las preferencias guardadas del usuario.
     * Si el usuario no tiene preferencias, devuelve un DTO vacío con onboardingCompleted=false.
     * No lanza 404 si no existen preferencias — solo si el usuario no existe.
     */
    @Transactional(readOnly = true)
    public UserPreferenceResponseDTO getUserPreferences(Long userId) {
        validateUserExists(userId);

        return userPreferenceRepository.findByUserId(userId)
                .map(userPreferenceMapper::toResponseDTO)
                .orElseGet(() -> userPreferenceMapper.emptyResponseDTO(userId));
    }

    // ─── Upsert ────────────────────────────────────────────────────────────────

    /**
     * Crea o actualiza las preferencias del usuario (upsert completo).
     * Reemplaza completamente las opciones y rangos previos.
     * Al guardar, marca onboardingCompleted=true en el usuario.
     */
    public UserPreferenceResponseDTO saveOrUpdateUserPreferences(UserPreferenceRequestDTO dto) {
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + dto.userId()));

        // Validar que todos los IDs de opciones existen
        List<PreferenceOption> selectedOptions = resolveOptions(dto.selectedOptionIds());

        // Obtener o crear la entidad UserPreference
        UserPreference pref = userPreferenceRepository.findByUserId(dto.userId())
                .orElseGet(() -> UserPreference.builder().user(user).build());

        // Reemplazar opciones seleccionadas
        Set<PreferenceOption> optionSet = new HashSet<>(selectedOptions);
        pref.setSelectedOptions(optionSet);

        // Reemplazar rangos
        pref.getRanges().clear();
        if (dto.ranges() != null && !dto.ranges().isEmpty()) {
            List<UserPreferenceRange> ranges = buildRanges(dto.ranges(), pref);
            pref.getRanges().addAll(ranges);
        }

        pref.setOnboardingCompleted(true);
        UserPreference saved = userPreferenceRepository.save(pref);

        // Marcar onboarding completado también en el User
        user.setOnboardingCompleted(true);
        userRepository.save(user);

        return userPreferenceMapper.toResponseDTO(saved);
    }

    // ─── Eliminación ───────────────────────────────────────────────────────────

    /**
     * Elimina todas las preferencias del usuario.
     */
    public void deleteUserPreferences(Long userId) {
        validateUserExists(userId);

        userPreferenceRepository.findByUserId(userId)
                .ifPresent(pref -> {
                    userPreferenceRepository.delete(pref);
                    // Resetear el flag de onboarding en el usuario
                    User user = pref.getUser();
                    user.setOnboardingCompleted(false);
                    userRepository.save(user);
                });
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    private List<PreferenceOption> resolveOptions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<PreferenceOption> found = preferenceOptionRepository.findAllById(ids);
        if (found.size() != ids.size()) {
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
