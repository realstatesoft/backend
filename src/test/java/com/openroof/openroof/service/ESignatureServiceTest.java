package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.SignLeaseRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ESignatureServiceTest {

    @Mock private LeaseRepository leaseRepository;
    @Mock private NotificationService notificationService;
    @Mock private LeaseService leaseService;

    private ESignatureService service;
    private Lease lease;

    @BeforeEach
    void setUp() {
        service = new ESignatureService(leaseRepository, notificationService, leaseService);

        User landlord = User.builder().email("owner@test.com").role(UserRole.USER).build();
        landlord.setId(1L);
        User tenant = User.builder().email("tenant@test.com").role(UserRole.USER).build();
        tenant.setId(2L);

        Property property = Property.builder().title("Depto Centro").owner(landlord).build();
        property.setId(100L);

        lease = Lease.builder()
                .property(property)
                .landlord(landlord)
                .primaryTenant(tenant)
                .status(LeaseStatus.DRAFT)
                .build();
        lease.setId(10L);
    }

    @Test
    @DisplayName("sendForSignature genera tokens, TTL y cambia estado")
    void sendForSignature_generatesTokensAndTtl() {
        when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
        when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

        Lease result = service.sendForSignature(10L);

        assertThat(result.getStatus()).isEqualTo(LeaseStatus.PENDING_SIGNATURE);
        assertThat(result.getSignatureTokenLandlord()).isNotNull();
        assertThat(result.getSignatureTokenTenant()).isNotNull();
        assertThat(result.getSignatureTokenExpiresAt()).isAfter(LocalDateTime.now().plusHours(71));
        verify(notificationService).notifyLeaseSentForSignature(result);
    }

    @Test
    @DisplayName("sendForSignature rechaza lease fuera de DRAFT")
    void sendForSignature_rejectsWrongStatus() {
        lease.setStatus(LeaseStatus.ACTIVE);
        when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

        assertThatThrownBy(() -> service.sendForSignature(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    @DisplayName("sign valida token, firma landlord y registra audit trail")
    @SuppressWarnings("unchecked")
    void sign_landlord_success() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        lease.setSignatureTokenLandlord(UUID.randomUUID());
        lease.setSignatureTokenTenant(UUID.randomUUID());
        lease.setSignatureTokenExpiresAt(LocalDateTime.now().plusHours(72));

        when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
        when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = lease.getSignatureTokenLandlord().toString();
        Lease result = service.sign(10L, token, new SignLeaseRequest("trace"), "127.0.0.1", "JUnit");

        assertThat(result.getSignedByLandlordAt()).isNotNull();
        assertThat(result.getSignatureTokenLandlord()).isNull();
        assertThat(result.getSignatureTokenTenant()).isNotNull();

        Map<String, Object> trail = result.getSignatureAuditTrail();
        assertThat(trail).containsKey("events");
        List<Map<String, Object>> events = (List<Map<String, Object>>) trail.get("events");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).get("signer")).isEqualTo("LANDLORD");
        assertThat(events.get(0).get("ip")).isEqualTo("127.0.0.1");
        assertThat(events.get(0).get("userAgent")).isEqualTo("JUnit");
        assertThat(events.get(0).get("tokenHash")).isNotNull();

        verify(notificationService).notifyLeaseSigned(result, NotificationService.SignerSide.LANDLORD);
        verify(leaseService, never()).activateLease(any());
    }

    @Test
    @DisplayName("sign activa lease cuando firma la segunda parte")
    void sign_secondSignature_activatesLease() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        lease.setSignatureTokenLandlord(UUID.randomUUID());
        lease.setSignatureTokenTenant(UUID.randomUUID());
        lease.setSignatureTokenExpiresAt(LocalDateTime.now().plusHours(72));
        lease.setSignedByLandlordAt(LocalDateTime.now().minusMinutes(2));

        when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
        when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));
        when(leaseService.activateLease(10L)).thenReturn(List.of());

        service.sign(10L, lease.getSignatureTokenTenant().toString(), null, "10.0.0.1", "JUnit");

        verify(leaseService).activateLease(10L);
        verify(notificationService).notifyLeaseSigned(lease, NotificationService.SignerSide.TENANT);
    }

    @Test
    @DisplayName("sign rechaza token expirado")
    void sign_rejectsExpiredToken() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        lease.setSignatureTokenLandlord(UUID.randomUUID());
        lease.setSignatureTokenTenant(UUID.randomUUID());
        lease.setSignatureTokenExpiresAt(LocalDateTime.now().minusSeconds(1));

        when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

        assertThatThrownBy(() -> service.sign(10L, lease.getSignatureTokenLandlord().toString(), null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("sign rechaza token de un solo uso ya consumido")
    void sign_rejectsConsumedToken() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        UUID landlordToken = UUID.randomUUID();
        lease.setSignatureTokenLandlord(landlordToken);
        lease.setSignatureTokenTenant(UUID.randomUUID());
        lease.setSignatureTokenExpiresAt(LocalDateTime.now().plusHours(72));

        when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
        when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

        service.sign(10L, landlordToken.toString(), null, null, null);

        assertThatThrownBy(() -> service.sign(10L, landlordToken.toString(), null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid signature token");
    }
}
