package com.openroof.openroof.service;

import com.openroof.openroof.config.ReservationProperties;
import com.openroof.openroof.dto.reservation.CancelReservationRequest;
import com.openroof.openroof.dto.reservation.CreateReservationRequest;
import com.openroof.openroof.dto.reservation.ReservationResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.ReservationStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.reservation.Reservation;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.ReservationRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.security.PropertySecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertySecurity propertySecurity;
    @Mock private NotificationService notificationService;

    private ReservationService service;

    private User buyer;
    private User owner;
    private Property property;

    @BeforeEach
    void setUp() {
        ReservationProperties props = new ReservationProperties(72, new BigDecimal("1.00"));
        service = new ReservationService(reservationRepository, propertyRepository,
                userRepository, propertySecurity, notificationService, props);

        buyer = User.builder().name("Comprador").email("buyer@test.com").role(UserRole.USER).build();
        buyer.setId(10L);

        owner = User.builder().name("Dueño").email("owner@test.com").role(UserRole.USER).build();
        owner.setId(20L);

        property = Property.builder()
                .title("Casa prueba")
                .owner(owner)
                .price(new BigDecimal("100000.00"))
                .status(PropertyStatus.PUBLISHED)
                .build();
        property.setId(100L);
    }

    @Nested
    @DisplayName("createReservation()")
    class Create {

        @Test
        @DisplayName("Crea reserva PENDING con expiresAt = now + ttlHours")
        void createsPendingWithTtl() {
            when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));
            when(reservationRepository.existsBlockingReservation(eq(100L), anyCollection()))
                    .thenReturn(false);
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(inv -> {
                        Reservation r = inv.getArgument(0);
                        r.setId(1L);
                        r.setCreatedAt(LocalDateTime.now());
                        r.setUpdatedAt(LocalDateTime.now());
                        return r;
                    });

            LocalDateTime before = LocalDateTime.now();
            ReservationResponse res = service.createReservation(
                    new CreateReservationRequest(100L, new BigDecimal("1000.00"), "Me interesa"),
                    "buyer@test.com");

            assertThat(res.status()).isEqualTo(ReservationStatus.PENDING);
            assertThat(res.buyerId()).isEqualTo(10L);
            assertThat(res.propertyId()).isEqualTo(100L);
            assertThat(res.expiresAt()).isAfter(before.plusHours(71));
            verify(notificationService).create(any(), eq("owner@test.com"));
        }

        @Test
        @DisplayName("Rechaza si la propiedad no está PUBLISHED")
        void rejectsNonPublished() {
            property.setStatus(PropertyStatus.ARCHIVED);
            when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));

            assertThatThrownBy(() -> service.createReservation(
                    new CreateReservationRequest(100L, new BigDecimal("1000.00"), null),
                    "buyer@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("publicada");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rechaza si el buyer es el owner")
        void rejectsOwnerAsBuyer() {
            when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(owner));
            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));

            assertThatThrownBy(() -> service.createReservation(
                    new CreateReservationRequest(100L, new BigDecimal("1000.00"), null),
                    "buyer@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("propia");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rechaza si ya existe una reserva PENDING o ACTIVE")
        void rejectsIfBlocked() {
            when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));
            when(reservationRepository.existsBlockingReservation(eq(100L), anyCollection()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createReservation(
                    new CreateReservationRequest(100L, new BigDecimal("1000.00"), null),
                    "buyer@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("reserva activa");
            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirmReservation()")
    class Confirm {

        @Test
        @DisplayName("Owner autorizado pasa reserva PENDING a ACTIVE y notifica al buyer")
        void confirmsPending() {
            Reservation r = baseReservation(ReservationStatus.PENDING);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(r));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReservationResponse res = service.confirmReservation(1L, "owner@test.com");

            assertThat(res.status()).isEqualTo(ReservationStatus.ACTIVE);
            verify(notificationService).create(any(), eq("buyer@test.com"));
        }

        @Test
        @DisplayName("Usuario sin permiso recibe ForbiddenException")
        void rejectsUnauthorized() {
            Reservation r = baseReservation(ReservationStatus.PENDING);
            User outsider = User.builder().email("evil@test.com").role(UserRole.USER).build();
            outsider.setId(999L);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(r));
            when(userRepository.findByEmail("evil@test.com")).thenReturn(Optional.of(outsider));
            when(propertySecurity.canModify(100L, outsider)).thenReturn(false);

            assertThatThrownBy(() -> service.confirmReservation(1L, "evil@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("cancelReservation()")
    class Cancel {

        @Test
        @DisplayName("Buyer puede cancelar su propia reserva guardando el motivo")
        void buyerCancels() {
            Reservation r = baseReservation(ReservationStatus.PENDING);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(r));
            when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReservationResponse res = service.cancelReservation(
                    1L, new CancelReservationRequest("Ya no me interesa"), "buyer@test.com");

            assertThat(res.status()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(res.cancelledReason()).isEqualTo("Ya no me interesa");
        }

        @Test
        @DisplayName("Rechaza cancelar una reserva CANCELLED")
        void rejectsAlreadyCancelled() {
            Reservation r = baseReservation(ReservationStatus.CANCELLED);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(r));
            when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));

            assertThatThrownBy(() -> service.cancelReservation(
                    1L, new CancelReservationRequest("x"), "buyer@test.com"))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("expireStaleReservations()")
    class Expire {

        @Test
        @DisplayName("Marca como EXPIRED todas las reservas vencidas y notifica")
        void expiresStale() {
            Reservation r1 = baseReservation(ReservationStatus.PENDING);
            r1.setExpiresAt(LocalDateTime.now().minusHours(1));
            Reservation r2 = baseReservation(ReservationStatus.ACTIVE);
            r2.setId(2L);
            r2.setExpiresAt(LocalDateTime.now().minusHours(2));

            when(reservationRepository.findExpired(anyCollection(), any()))
                    .thenReturn(List.of(r1, r2));
            when(reservationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            int expired = service.expireStaleReservations();

            assertThat(expired).isEqualTo(2);
            assertThat(r1.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
            assertThat(r2.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("convertToContract()")
    class Convert {

        @Test
        @DisplayName("Transiciona ACTIVE a CONVERTED_TO_CONTRACT con autorización")
        void convertsActive() {
            Reservation r = baseReservation(ReservationStatus.ACTIVE);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(r));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReservationResponse res = service.convertToContract(1L, "owner@test.com");

            assertThat(res.status()).isEqualTo(ReservationStatus.CONVERTED_TO_CONTRACT);
        }

        @Test
        @DisplayName("Rechaza convertir si la reserva no está ACTIVE")
        void rejectsNonActive() {
            Reservation r = baseReservation(ReservationStatus.PENDING);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(r));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);

            assertThatThrownBy(() -> service.convertToContract(1L, "owner@test.com"))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetById {
        @Test
        @DisplayName("Devuelve 404 si no existe")
        void notFound() {
            when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(1L, "buyer@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private Reservation baseReservation(ReservationStatus status) {
        Reservation r = Reservation.builder()
                .property(property)
                .buyer(buyer)
                .reservationAmount(new BigDecimal("1000.00"))
                .status(status)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        r.setId(1L);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    @Nested
    @DisplayName("getMyReservationForProperty()")
    class MyReservationForPropertyTests {

        @Test
        @DisplayName("Devuelve la reserva PENDIENTE/ACTIVA del comprador para la propiedad")
        void returnsActiveReservationForBuyer() {
            when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
            Reservation r = Reservation.builder()
                    .property(property).buyer(buyer)
                    .reservationAmount(new BigDecimal("1000"))
                    .status(ReservationStatus.PENDING)
                    .build();
            r.setId(77L);
            when(reservationRepository.findFirstByProperty_IdAndBuyer_IdAndStatusInOrderByCreatedAtDesc(
                    eq(property.getId()), eq(buyer.getId()), anyCollection()))
                    .thenReturn(Optional.of(r));

            Optional<ReservationResponse> result =
                    service.getMyReservationForProperty(property.getId(), "buyer@test.com");

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(77L);
            assertThat(result.get().status()).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("Devuelve vacío cuando el comprador no tiene reserva activa para la propiedad")
        void returnsEmptyWhenNoActiveReservation() {
            when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
            when(reservationRepository.findFirstByProperty_IdAndBuyer_IdAndStatusInOrderByCreatedAtDesc(
                    anyLong(), anyLong(), anyCollection()))
                    .thenReturn(Optional.empty());

            assertThat(service.getMyReservationForProperty(property.getId(), "buyer@test.com"))
                    .isEmpty();
        }
    }
}