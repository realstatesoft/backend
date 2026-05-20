package com.openroof.openroof.service;

import com.openroof.openroof.audit.AuditPayloadSanitizer;
import com.openroof.openroof.dto.admin.AuditEntityOptionResponse;
import com.openroof.openroof.model.admin.AuditLog;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.enums.AuditAction;
import com.openroof.openroof.model.enums.AuditEntityType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AuditLogRepository;
import com.openroof.openroof.repository.ContractRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService")
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository      userRepository;
    @Mock private PropertyRepository  propertyRepository;
    @Mock private ContractRepository  contractRepository;

    @InjectMocks private AuditService auditService;

    // ─── suggestAuditEntities — limit clamping ───────────────────────────────

    @Test
    @DisplayName("suggestAuditEntities_limitAboveMax_clampedTo50")
    @SuppressWarnings("unchecked")
    void suggestAuditEntities_limitAboveMax_clampedTo50() {
        Page<User> page = mock(Page.class);
        when(page.stream()).thenReturn(Stream.empty());
        when(userRepository.searchForAuditPicker(anyString(), any(Pageable.class))).thenReturn(page);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        auditService.suggestAuditEntities(AuditEntityType.USER, "test", 100);

        verify(userRepository).searchForAuditPicker(anyString(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("suggestAuditEntities_limitBelowMin_clampedTo1")
    @SuppressWarnings("unchecked")
    void suggestAuditEntities_limitBelowMin_clampedTo1() {
        Page<User> page = mock(Page.class);
        when(page.stream()).thenReturn(Stream.empty());
        when(userRepository.searchForAuditPicker(anyString(), any(Pageable.class))).thenReturn(page);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        auditService.suggestAuditEntities(AuditEntityType.USER, "test", 0);

        verify(userRepository).searchForAuditPicker(anyString(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("suggestAuditEntities_queryLongerThan80Chars_truncated")
    @SuppressWarnings("unchecked")
    void suggestAuditEntities_queryLongerThan80Chars_truncated() {
        Page<User> page = mock(Page.class);
        when(page.stream()).thenReturn(Stream.empty());
        when(userRepository.searchForAuditPicker(anyString(), any(Pageable.class))).thenReturn(page);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        String longQuery = "A".repeat(100);

        auditService.suggestAuditEntities(AuditEntityType.USER, longQuery, 5);

        verify(userRepository).searchForAuditPicker(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue()).hasSize(80);
    }

    // ─── suggestAuditEntities — entity type routing ──────────────────────────

    @Test
    @DisplayName("suggestAuditEntities_propertyType_callsPropertyRepo")
    @SuppressWarnings("unchecked")
    void suggestAuditEntities_propertyType_callsPropertyRepo() {
        Page<Property> page = mock(Page.class);
        when(page.stream()).thenReturn(Stream.empty());
        when(propertyRepository.searchByTitleForAuditPicker(anyString(), any(Pageable.class))).thenReturn(page);

        List<AuditEntityOptionResponse> result =
                auditService.suggestAuditEntities(AuditEntityType.PROPERTY, "casa", 5);

        verify(propertyRepository).searchByTitleForAuditPicker(anyString(), any(Pageable.class));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("suggestAuditEntities_contractNumericQuery_passesIdToRepo")
    @SuppressWarnings("unchecked")
    void suggestAuditEntities_contractNumericQuery_passesIdToRepo() {
        Page<Contract> page = mock(Page.class);
        when(page.stream()).thenReturn(Stream.empty());
        when(contractRepository.searchForAuditPicker(anyString(), anyLong(), any(Pageable.class))).thenReturn(page);

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);

        auditService.suggestAuditEntities(AuditEntityType.CONTRACT, "42", 5);

        verify(contractRepository).searchForAuditPicker(anyString(), idCaptor.capture(), any(Pageable.class));
        assertThat(idCaptor.getValue()).isEqualTo(42L);
    }

    @Test
    @DisplayName("suggestAuditEntities_contractNonNumericQuery_passesNegativeOneToRepo")
    @SuppressWarnings("unchecked")
    void suggestAuditEntities_contractNonNumericQuery_passesNegativeOneToRepo() {
        Page<Contract> page = mock(Page.class);
        when(page.stream()).thenReturn(Stream.empty());
        when(contractRepository.searchForAuditPicker(anyString(), anyLong(), any(Pageable.class))).thenReturn(page);

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);

        auditService.suggestAuditEntities(AuditEntityType.CONTRACT, "not-a-number", 5);

        verify(contractRepository).searchForAuditPicker(anyString(), idCaptor.capture(), any(Pageable.class));
        assertThat(idCaptor.getValue()).isEqualTo(-1L);
    }

    // ─── suggestAuditEntities — label formatting (via USER results) ──────────

    @Test
    @DisplayName("suggestAuditEntities_userWithName_labelIncludesNameAndEmail")
    @SuppressWarnings("unchecked")
    void suggestAuditEntities_userWithName_labelIncludesNameAndEmail() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getName()).thenReturn("Ana Torres");
        when(user.getEmail()).thenReturn("ana@example.com");

        Page<User> page = mock(Page.class);
        when(page.stream()).thenReturn(Stream.of(user));
        when(userRepository.searchForAuditPicker(anyString(), any(Pageable.class))).thenReturn(page);

        List<AuditEntityOptionResponse> result =
                auditService.suggestAuditEntities(AuditEntityType.USER, "Ana", 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).label()).isEqualTo("Ana Torres (ana@example.com)");
    }

    @Test
    @DisplayName("suggestAuditEntities_userBlankName_labelIsEmailOnly")
    @SuppressWarnings("unchecked")
    void suggestAuditEntities_userBlankName_labelIsEmailOnly() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(2L);
        when(user.getName()).thenReturn("  ");
        when(user.getEmail()).thenReturn("noname@example.com");

        Page<User> page = mock(Page.class);
        when(page.stream()).thenReturn(Stream.of(user));
        when(userRepository.searchForAuditPicker(anyString(), any(Pageable.class))).thenReturn(page);

        List<AuditEntityOptionResponse> result =
                auditService.suggestAuditEntities(AuditEntityType.USER, "", 5);

        assertThat(result.get(0).label()).isEqualTo("noname@example.com");
    }

    // ─── log — no HTTP request context ──────────────────────────────────────

    @Test
    @DisplayName("log_withoutRequestContext_savesEntryWithNullIpAndUA")
    void log_withoutRequestContext_savesEntryWithNullIpAndUA() {
        // RequestContextHolder has no bound request outside Spring — ip/ua are null
        User actor = mock(User.class);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        auditService.log(
                actor,
                AuditEntityType.PROPERTY,
                42L,
                AuditAction.CREATE,
                null,
                Map.of("title", "Nueva Propiedad"));

        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo("PROPERTY");
        assertThat(saved.getEntityId()).isEqualTo(42L);
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getIpAddress()).isNull();
        assertThat(saved.getUserAgent()).isNull();
        assertThat(saved.getUser()).isSameAs(actor);
    }

    @Test
    @DisplayName("log_sensitivenewValues_redactedBeforePersist")
    void log_sensitiveNewValues_redactedBeforePersist() {
        User actor = mock(User.class);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        auditService.log(
                actor,
                AuditEntityType.USER,
                1L,
                AuditAction.UPDATE,
                null,
                Map.of("email", "new@example.com", "password", "secret123"));

        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getNewValues()).containsEntry("password", "[redacted]");
        assertThat(saved.getNewValues()).containsEntry("email", "new@example.com");
    }
}
