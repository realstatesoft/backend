package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentSocialMedia;
import com.openroof.openroof.model.agent.AgentSpecialty;
import com.openroof.openroof.model.enums.SocialMediaPlatform;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AgentProfileMapperTest {

    private AgentProfileMapper mapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        mapper = new AgentProfileMapper();
        testUser = User.builder()
                .email("agent@test.com")
                .passwordHash("hashed")
                .name("Test Agent")
                .phone("+1234567890")
                .role(UserRole.AGENT)
                .build();
        testUser.setId(1L);
    }

    @Test
    @DisplayName("toResponse() mapea todos los campos correctamente")
    void toResponse_mapsAllFields() {
        AgentSpecialty spec = AgentSpecialty.builder().name("Residential").build();
        spec.setId(1L);

        AgentProfile agent = AgentProfile.builder()
                .user(testUser)
                .companyName("Test Realty")
                .bio("Bio text")
                .experienceYears(5)
                .licenseNumber("LIC-001")
                .avgRating(new BigDecimal("4.50"))
                .totalReviews(10)
                .specialties(List.of(spec))
                .socialMedia(new ArrayList<>())
                .build();
        agent.setId(10L);

        AgentSocialMedia sm = AgentSocialMedia.builder()
                .agent(agent)
                .platform(SocialMediaPlatform.LINKEDIN)
                .url("https://linkedin.com/test")
                .build();
        agent.getSocialMedia().add(sm);

        AgentProfileResponse.AgentStatsDto stats = new AgentProfileResponse.AgentStatsDto(5, 2, 7, "$ 500.000");
        AgentProfileResponse response = mapper.toResponse(agent, stats);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.userName()).isEqualTo("Test Agent");
        assertThat(response.userEmail()).isEqualTo("agent@test.com");
        assertThat(response.companyName()).isEqualTo("Test Realty");
        assertThat(response.licenseNumber()).isEqualTo("LIC-001");
        assertThat(response.avgRating()).isEqualByComparingTo("4.50");
        assertThat(response.totalReviews()).isEqualTo(10);
        assertThat(response.specialties()).hasSize(1);
        assertThat(response.specialties().get(0).name()).isEqualTo("Residential");
        assertThat(response.socialMedia()).hasSize(1);
        assertThat(response.socialMedia().get(0).platform()).isEqualTo(SocialMediaPlatform.LINKEDIN);
    }

    @Test
    @DisplayName("toSummaryResponse() mapea campos resumen")
    void toSummaryResponse_mapsSummaryFields() {
        AgentSpecialty spec = AgentSpecialty.builder().name("residencial").build();
        spec.setId(1L);
        
        AgentProfile agent = AgentProfile.builder()
                .user(testUser)
                .companyName("Test Realty")
                .experienceYears(5)
                .licenseNumber("LIC-001")
                .avgRating(BigDecimal.ZERO)
                .totalReviews(0)
                .specialties(List.of(spec))
                .socialMedia(new ArrayList<>())
                .build();
        agent.setId(10L);

        AgentProfileSummaryResponse summary = mapper.toSummaryResponse(agent);

        assertThat(summary.id()).isEqualTo(10L);
        assertThat(summary.userName()).isEqualTo("Test Agent");
        assertThat(summary.companyName()).isEqualTo("Test Realty");
        assertThat(summary.licenseNumber()).isEqualTo("LIC-001");
        assertThat(summary.specialties()).hasSize(1);
        assertThat(summary.specialties().get(0)).isEqualTo("residencial");
    }

    @Test
    @DisplayName("toEntity() crea entidad desde request")
    void toEntity_createsEntityFromRequest() {
        var socialMedia = List.of(
                new AgentSocialMediaDto(SocialMediaPlatform.FACEBOOK, "https://facebook.com/test")
        );
        var request = new CreateAgentProfileRequest(
                1L, "Company", "Bio", 3, "LIC-002",
                null, socialMedia
        );

        AgentSpecialty spec = AgentSpecialty.builder().name("Commercial").build();
        spec.setId(2L);

        AgentProfile agent = mapper.toEntity(request, testUser, List.of(spec));

        assertThat(agent.getUser()).isEqualTo(testUser);
        assertThat(agent.getCompanyName()).isEqualTo("Company");
        assertThat(agent.getBio()).isEqualTo("Bio");
        assertThat(agent.getExperienceYears()).isEqualTo(3);
        assertThat(agent.getLicenseNumber()).isEqualTo("LIC-002");
        assertThat(agent.getSpecialties()).hasSize(1);
        assertThat(agent.getSocialMedia()).hasSize(1);
        assertThat(agent.getSocialMedia().get(0).getPlatform()).isEqualTo(SocialMediaPlatform.FACEBOOK);
    }

    @Test
    @DisplayName("updateEntity() actualiza solo campos no nulos")
    void updateEntity_updatesOnlyNonNullFields() {
        AgentProfile agent = AgentProfile.builder()
                .user(testUser)
                .companyName("Old Company")
                .bio("Old bio")
                .experienceYears(5)
                .licenseNumber("LIC-OLD")
                .specialties(new ArrayList<>())
                .socialMedia(new ArrayList<>())
                .build();

        var request = new UpdateAgentProfileRequest(
                "New Company", null, 10, null, null, null
        );

        mapper.updateEntity(agent, request);

        assertThat(agent.getCompanyName()).isEqualTo("New Company");
        assertThat(agent.getBio()).isEqualTo("Old bio"); // no cambió
        assertThat(agent.getExperienceYears()).isEqualTo(10);
        assertThat(agent.getLicenseNumber()).isEqualTo("LIC-OLD"); // no cambió
    }

    @Test
    @DisplayName("replaceSocialMedia() reemplaza redes sociales")
    void replaceSocialMedia_replaces() {
        AgentProfile agent = AgentProfile.builder()
                .user(testUser)
                .companyName("Company")
                .specialties(new ArrayList<>())
                .socialMedia(new ArrayList<>())
                .build();
        agent.setId(10L);

        // Add initial
        agent.getSocialMedia().add(AgentSocialMedia.builder()
                .agent(agent)
                .platform(SocialMediaPlatform.FACEBOOK)
                .url("https://facebook.com/old")
                .build());

        var newMedia = List.of(
                new AgentSocialMediaDto(SocialMediaPlatform.INSTAGRAM, "https://instagram.com/new")
        );

        mapper.replaceSocialMedia(agent, newMedia);

        assertThat(agent.getSocialMedia()).hasSize(1);
        assertThat(agent.getSocialMedia().get(0).getPlatform()).isEqualTo(SocialMediaPlatform.INSTAGRAM);
        assertThat(agent.getSocialMedia().get(0).getUrl()).isEqualTo("https://instagram.com/new");
    }
}
