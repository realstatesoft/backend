package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentSocialMedia;
import com.openroof.openroof.model.agent.AgentSpecialty;
import com.openroof.openroof.model.user.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper manual para conversiones entre AgentProfile (entidad) y DTOs.
 */
@Component
public class AgentProfileMapper {

    // ─── Entity → Response ────────────────────────────────────────

    public AgentProfileResponse toResponse(AgentProfile agent, AgentProfileResponse.AgentStatsDto stats) {
        User user = agent.getUser();

        return new AgentProfileResponse(
                agent.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                user != null ? user.getAvatarUrl() : null,
                agent.getCompanyName(),
                agent.getBio(),
                agent.getExperienceYears(),
                agent.getLicenseNumber(),
                agent.getAvgRating(),
                agent.getTotalReviews(),
                mapSpecialties(agent.getSpecialties()),
                mapSocialMedia(agent.getSocialMedia()),
                stats,
                agent.getCreatedAt(),
                agent.getUpdatedAt()
        );
    }

    public AgentProfileSummaryResponse toSummaryResponse(AgentProfile agent) {
        User user = agent.getUser();
        return new AgentProfileSummaryResponse(
                agent.getId(),
                user != null ? user.getName() : null,
                user != null ? user.getPhone() : null,
                user != null ? user.getAvatarUrl() : null,
                agent.getCompanyName(),
                agent.getExperienceYears(),
                agent.getLicenseNumber(),
                agent.getAvgRating(),
                agent.getTotalReviews(),
                mapSpecialtyNames(agent.getSpecialties())
        );
    }

    private List<String> mapSpecialtyNames(List<AgentSpecialty> specialties) {
        if (specialties == null) return List.of();
        return specialties.stream()
                .map(AgentSpecialty::getName)
                .toList();
    }

    // ─── Request → Entity ─────────────────────────────────────────

    public AgentProfile toEntity(CreateAgentProfileRequest request, User user, List<AgentSpecialty> specialties) {
        AgentProfile agent = AgentProfile.builder()
                .user(user)
                .companyName(request.companyName())
                .bio(request.bio())
                .experienceYears(request.experienceYears())
                .licenseNumber(request.licenseNumber())
                .specialties(specialties != null ? specialties : Collections.emptyList())
                .build();

        // Agregar redes sociales
        if (request.socialMedia() != null && !request.socialMedia().isEmpty()) {
            List<AgentSocialMedia> socialMediaList = request.socialMedia().stream()
                    .map(dto -> AgentSocialMedia.builder()
                            .agent(agent)
                            .platform(dto.platform())
                            .url(dto.url())
                            .build())
                    .toList();
            agent.getSocialMedia().addAll(socialMediaList);
        }

        return agent;
    }

    // ─── Actualización parcial ────────────────────────────────────

    public void updateEntity(AgentProfile agent, UpdateAgentProfileRequest request) {
        if (request.companyName() != null)
            agent.setCompanyName(request.companyName());
        if (request.bio() != null)
            agent.setBio(request.bio());
        if (request.experienceYears() != null)
            agent.setExperienceYears(request.experienceYears());
        if (request.licenseNumber() != null)
            agent.setLicenseNumber(request.licenseNumber());
    }

    public void replaceSocialMedia(AgentProfile agent, List<AgentSocialMediaDto> socialMediaDtos) {
        agent.getSocialMedia().clear();
        if (socialMediaDtos != null && !socialMediaDtos.isEmpty()) {
            List<AgentSocialMedia> newMedia = socialMediaDtos.stream()
                    .map(dto -> AgentSocialMedia.builder()
                            .agent(agent)
                            .platform(dto.platform())
                            .url(dto.url())
                            .build())
                    .toList();
            agent.getSocialMedia().addAll(newMedia);
        }
    }

    // ─── Helpers privados ─────────────────────────────────────────

    private List<AgentProfileResponse.SpecialtyDto> mapSpecialties(List<AgentSpecialty> specialties) {
        if (specialties == null) return Collections.emptyList();
        return specialties.stream()
                .map(s -> new AgentProfileResponse.SpecialtyDto(s.getId(), s.getName()))
                .toList();
    }

    private List<AgentSocialMediaDto> mapSocialMedia(List<AgentSocialMedia> socialMedia) {
        if (socialMedia == null) return Collections.emptyList();
        return socialMedia.stream()
                .map(sm -> new AgentSocialMediaDto(sm.getPlatform(), sm.getUrl()))
                .toList();
    }
}
