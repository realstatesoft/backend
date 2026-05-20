package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.SignLeaseRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("ESignatureService — pruebas de seguridad de tokens")
class ESignatureServiceSecurityTest {

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
                .status(LeaseStatus.PENDING_SIGNATURE)
                .signatureTokenLandlord(UUID.randomUUID())
                .signatureTokenTenant(UUID.randomUUID())
                .signatureTokenExpiresAt(LocalDateTime.now().plusHours(72))
                .build();
        lease.setId(10L);
    }

    @Nested
    @DisplayName("Validación de token")
    class TokenValidation {

        @Test
        @DisplayName("Firmar con token nulo retorna 400 (BadRequestException)")
        void signWithNullTokenThrowsBadRequest() {
            assertThatThrownBy(() -> service.sign(10L, null, null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Signature token is required");

            verify(leaseRepository, never()).findByIdForUpdate(any());
        }

        @Test
        @DisplayName("Firmar con token vacío retorna 400 (BadRequestException)")
        void signWithBlankTokenThrowsBadRequest() {
            assertThatThrownBy(() -> service.sign(10L, "   ", null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Signature token is required");

            verify(leaseRepository, never()).findByIdForUpdate(any());
        }

        @Test
        @DisplayName("Firmar con token UUID inválido retorna 400 (BadRequestException)")
        void signWithInvalidUuidThrowsBadRequest() {
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.sign(10L, "not-a-valid-uuid", null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid signature token");
        }

        @Test
        @DisplayName("Firmar con token que no coincide con ningún lado retorna 400")
        void signWithNonMatchingTokenThrowsBadRequest() {
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

            UUID unknownToken = UUID.randomUUID();
            assertThatThrownBy(() -> service.sign(10L, unknownToken.toString(), null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid signature token");
        }
    }

    @Nested
    @DisplayName("Caducidad y token de un solo uso")
    class ExpiryAndSingleUse {

        @Test
        @DisplayName("Firmar con token expirado retorna 400 con mensaje de expiración")
        void signWithExpiredTokenThrowsBadRequest() {
            lease.setSignatureTokenExpiresAt(LocalDateTime.now().minusSeconds(1));
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.sign(10L, lease.getSignatureTokenLandlord().toString(), null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Firmar con signatureTokenExpiresAt nulo retorna 400 con mensaje de expiración")
        void signWithNullExpiresAtThrowsBadRequest() {
            lease.setSignatureTokenExpiresAt(null);
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.sign(10L, lease.getSignatureTokenLandlord().toString(), null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Firmar mismo token dos veces consecutivas retorna 400 (uso único)")
        void signSameTokenTwiceThrowsBadRequest() {
            UUID landlordToken = lease.getSignatureTokenLandlord();
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

            service.sign(10L, landlordToken.toString(), null, null, null);
            assertThat(lease.getSignatureTokenLandlord()).isNull();

            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.sign(10L, landlordToken.toString(), null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid signature token");
        }

        @Test
        @DisplayName("Firmar con token de tenant que ya fue consumido retorna 400")
        void signConsumedTenantTokenThrowsBadRequest() {
            UUID tenantToken = lease.getSignatureTokenTenant();
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

            service.sign(10L, tenantToken.toString(), null, null, null);
            assertThat(lease.getSignatureTokenTenant()).isNull();

            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.sign(10L, tenantToken.toString(), null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid signature token");
        }
    }

    @Nested
    @DisplayName("Errores de lease")
    class LeaseErrors {

        @Test
        @DisplayName("Firmar con ID de lease inexistente retorna 404 (ResourceNotFoundException)")
        void signWithNonExistentLeaseIdThrowsNotFound() {
            when(leaseRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.sign(999L, UUID.randomUUID().toString(), null, null, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Lease");
        }

        @Test
        @DisplayName("Firmar lease que no está en PENDING_SIGNATURE retorna 400")
        void signLeaseNotPendingSignatureThrowsBadRequest() {
            lease.setStatus(LeaseStatus.DRAFT);
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.sign(10L, lease.getSignatureTokenLandlord().toString(), null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("PENDING_SIGNATURE");
        }

        @Test
        @DisplayName("Firmar como landlord cuando ya firmó landlord retorna 400")
        void signAlreadySignedByLandlordThrowsBadRequest() {
            lease.setSignedByLandlordAt(LocalDateTime.now().minusMinutes(5));
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.sign(10L, lease.getSignatureTokenLandlord().toString(), null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already signed by landlord");
        }

        @Test
        @DisplayName("Firmar como tenant cuando ya firmó tenant retorna 400")
        void signAlreadySignedByTenantThrowsBadRequest() {
            lease.setSignedByTenantAt(LocalDateTime.now().minusMinutes(5));
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.sign(10L, lease.getSignatureTokenTenant().toString(), null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already signed by tenant");
        }
    }

    @Nested
    @DisplayName("Audit Trail")
    class AuditTrail {

        @Test
        @DisplayName("Registra ipAddress, userAgent y timestamp en signatureAuditTrail")
        @SuppressWarnings("unchecked")
        void signRecordsIpUserAgentAndTimestamp() {
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

            String token = lease.getSignatureTokenLandlord().toString();
            Lease result = service.sign(10L, token, null, "192.168.1.50", "Mozilla/5.0 TestAgent");

            Map<String, Object> trail = result.getSignatureAuditTrail();
            assertThat(trail).isNotNull();
            assertThat(trail).containsKey("events");

            List<Map<String, Object>> events = (List<Map<String, Object>>) trail.get("events");
            assertThat(events).hasSize(1);

            Map<String, Object> event = events.get(0);
            assertThat(event.get("ip")).isEqualTo("192.168.1.50");
            assertThat(event.get("userAgent")).isEqualTo("Mozilla/5.0 TestAgent");
            assertThat(event.get("timestamp")).isNotNull();
            assertThat(event.get("signer")).isEqualTo("LANDLORD");
        }

        @Test
        @DisplayName("Registra tokenHash en audit trail sin exponer token en texto plano")
        @SuppressWarnings("unchecked")
        void signRecordsTokenHashNotPlaintext() {
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

            String landLordToken = lease.getSignatureTokenLandlord().toString();
            Lease result = service.sign(10L, landLordToken, null, "10.0.0.1", "JUnit");

            Map<String, Object> trail = result.getSignatureAuditTrail();
            List<Map<String, Object>> events = (List<Map<String, Object>>) trail.get("events");
            String tokenHash = (String) events.get(0).get("tokenHash");

            assertThat(tokenHash).isNotNull();
            assertThat(tokenHash).isNotEqualTo(landLordToken);
            assertThat(tokenHash).hasSize(64);
        }

        @Test
        @DisplayName("Acumula eventos consecutivos en audit trail tras firma de ambas partes")
        @SuppressWarnings("unchecked")
        void signAccumulatesBothSignaturesInAuditTrail() {
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

            service.sign(10L, lease.getSignatureTokenLandlord().toString(), null, "10.0.0.1", "JUnit");
            service.sign(10L, lease.getSignatureTokenTenant().toString(), null, "10.0.0.2", "JUnit");

            Map<String, Object> trail = lease.getSignatureAuditTrail();
            List<Map<String, Object>> events = (List<Map<String, Object>>) trail.get("events");
            assertThat(events).hasSize(2);

            assertThat(events.get(0).get("signer")).isEqualTo("LANDLORD");
            assertThat(events.get(0).get("ip")).isEqualTo("10.0.0.1");
            assertThat(events.get(1).get("signer")).isEqualTo("TENANT");
            assertThat(events.get(1).get("ip")).isEqualTo("10.0.0.2");
        }

        @Test
        @DisplayName("Registra signatureData cuando se envía en el request")
        @SuppressWarnings("unchecked")
        void signRecordsSignatureDataWhenProvided() {
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

            SignLeaseRequest request = new SignLeaseRequest("data:image/png;base64,iVBORw0KGgo=");
            Lease result = service.sign(10L, lease.getSignatureTokenLandlord().toString(), request, "10.0.0.1", "JUnit");

            Map<String, Object> trail = result.getSignatureAuditTrail();
            List<Map<String, Object>> events = (List<Map<String, Object>>) trail.get("events");
            assertThat(events.get(0).get("signatureData")).isEqualTo("data:image/png;base64,iVBORw0KGgo=");
        }

        @Test
        @DisplayName("No registra signatureData cuando el request es null")
        @SuppressWarnings("unchecked")
        void signDoesNotRecordSignatureDataWhenNull() {
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

            Lease result = service.sign(10L, lease.getSignatureTokenLandlord().toString(), null, "10.0.0.1", "JUnit");

            Map<String, Object> trail = result.getSignatureAuditTrail();
            List<Map<String, Object>> events = (List<Map<String, Object>>) trail.get("events");
            assertThat(events.get(0)).doesNotContainKey("signatureData");
        }

        @Test
        @DisplayName("Verifica que el Lease guardado en repository contiene el audit trail completo")
        @SuppressWarnings("unchecked")
        void signVerifiesSavedLeaseHasCompleteAuditTrail() {
            when(leaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

            service.sign(10L, lease.getSignatureTokenLandlord().toString(), new SignLeaseRequest("sig-data"), "192.168.0.1", "TestBrowser/1.0");

            ArgumentCaptor<Lease> savedLeaseCaptor = ArgumentCaptor.forClass(Lease.class);
            verify(leaseRepository).save(savedLeaseCaptor.capture());

            Lease saved = savedLeaseCaptor.getValue();
            Map<String, Object> trail = saved.getSignatureAuditTrail();
            List<Map<String, Object>> events = (List<Map<String, Object>>) trail.get("events");
            Map<String, Object> event = events.get(0);

            assertThat(event.get("ip")).isEqualTo("192.168.0.1");
            assertThat(event.get("userAgent")).isEqualTo("TestBrowser/1.0");
            assertThat(event.get("signatureData")).isEqualTo("sig-data");
            assertThat(event.get("tokenHash")).isNotNull();
            assertThat(event.get("timestamp")).isNotNull();
            assertThat(event.get("signer")).isEqualTo("LANDLORD");
        }
    }
}
