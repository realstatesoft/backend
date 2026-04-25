package com.openroof.openroof.service;

import com.openroof.openroof.dto.payment.PaymentRequest;
import com.openroof.openroof.dto.payment.PaymentResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.enums.PaymentType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PaymentRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;

    private PaymentService service;

    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, userRepository);

        user = User.builder().name("Juan Pérez").email("user@test.com").role(UserRole.USER).build();
        user.setId(1L);

        admin = User.builder().name("Admin").email("admin@test.com").role(UserRole.ADMIN).build();
        admin.setId(2L);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Payment buildPayment(Long id, User owner, PaymentStatus status) {
        Payment p = Payment.builder()
                .user(owner)
                .type(PaymentType.RESERVATION)
                .status(status)
                .concept("Señal de reserva")
                .amount(new BigDecimal("500.00"))
                .transactionCode("uuid-test-" + id)
                .build();
        p.setId(id);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Crea un pago con estado PENDING y transactionCode generado")
        void createsPendingPayment() {
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(10L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            PaymentRequest request = new PaymentRequest(
                    PaymentType.RESERVATION, new BigDecimal("500.00"), "Señal de reserva");

            PaymentResponse response = service.create(request, "user@test.com");

            assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
            assertThat(response.type()).isEqualTo(PaymentType.RESERVATION);
            assertThat(response.amount()).isEqualByComparingTo("500.00");
            assertThat(response.concept()).isEqualTo("Señal de reserva");
            assertThat(response.transactionCode()).isNotBlank();
            assertThat(response.userId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("El concepto se almacena sin espacios extremos (trim)")
        void trimsConcept() {
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(11L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            PaymentRequest request = new PaymentRequest(
                    PaymentType.SUBSCRIPTION, new BigDecimal("100.00"), "  Suscripción mensual  ");

            PaymentResponse response = service.create(request, "user@test.com");

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getConcept()).isEqualTo("Suscripción mensual");
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el usuario no existe")
        void throwsWhenUserNotFound() {
            when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(
                    new PaymentRequest(PaymentType.RESERVATION, new BigDecimal("100.00"), "Test"),
                    "nobody@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario");
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("El propio usuario puede ver su pago")
        void ownerCanSeeOwnPayment() {
            Payment payment = buildPayment(1L, user, PaymentStatus.PENDING);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            PaymentResponse response = service.getById(1L, "user@test.com");

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("ADMIN puede ver el pago de cualquier usuario")
        void adminCanSeeAnyPayment() {
            Payment payment = buildPayment(1L, user, PaymentStatus.APPROVED);
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            PaymentResponse response = service.getById(1L, "admin@test.com");

            assertThat(response.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Otro usuario lanza ForbiddenException")
        void otherUserThrowsForbidden() {
            User otherUser = User.builder().email("other@test.com").role(UserRole.USER).build();
            otherUser.setId(99L);
            Payment payment = buildPayment(1L, user, PaymentStatus.PENDING);
            when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> service.getById(1L, "other@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el pago no existe")
        void throwsWhenNotFound() {
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L, "user@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Pago");
        }
    }

    @Nested
    @DisplayName("getMyPayments()")
    class GetMyPayments {

        @Test
        @DisplayName("Devuelve todos los pagos del usuario sin filtro de estado")
        void returnsAllPaymentsWithoutFilter() {
            Pageable pageable = PageRequest.of(0, 10);
            Payment payment = buildPayment(1L, user, PaymentStatus.PENDING);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(paymentRepository.findByUser_Id(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(payment)));

            Page<PaymentResponse> result = service.getMyPayments("user@test.com", null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Filtra por estado cuando se proporciona")
        void filtersByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Payment payment = buildPayment(1L, user, PaymentStatus.APPROVED);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(paymentRepository.findByUser_IdAndStatus(1L, PaymentStatus.APPROVED, pageable))
                    .thenReturn(new PageImpl<>(List.of(payment)));

            Page<PaymentResponse> result = service.getMyPayments("user@test.com", PaymentStatus.APPROVED, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).status()).isEqualTo(PaymentStatus.APPROVED);
        }

        @Test
        @DisplayName("Devuelve página vacía si el usuario no tiene pagos")
        void returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(paymentRepository.findByUser_Id(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<PaymentResponse> result = service.getMyPayments("user@test.com", null, pageable);

            assertThat(result.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("Sin filtros devuelve todos los pagos")
        void returnsAllWithoutFilters() {
            Pageable pageable = PageRequest.of(0, 20);
            when(paymentRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(
                            buildPayment(1L, user, PaymentStatus.PENDING),
                            buildPayment(2L, admin, PaymentStatus.APPROVED))));

            Page<PaymentResponse> result = service.getAll(null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Filtra solo por userId")
        void filtersByUserId() {
            Pageable pageable = PageRequest.of(0, 20);
            when(paymentRepository.findByUser_Id(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(buildPayment(1L, user, PaymentStatus.PENDING))));

            Page<PaymentResponse> result = service.getAll(1L, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).userId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Filtra solo por status")
        void filtersByStatus() {
            Pageable pageable = PageRequest.of(0, 20);
            when(paymentRepository.findByStatus(PaymentStatus.REJECTED, pageable))
                    .thenReturn(new PageImpl<>(List.of(buildPayment(3L, user, PaymentStatus.REJECTED))));

            Page<PaymentResponse> result = service.getAll(null, PaymentStatus.REJECTED, pageable);

            assertThat(result.getContent().get(0).status()).isEqualTo(PaymentStatus.REJECTED);
        }

        @Test
        @DisplayName("Filtra por userId y status combinados")
        void filtersByUserIdAndStatus() {
            Pageable pageable = PageRequest.of(0, 20);
            when(paymentRepository.findByUser_IdAndStatus(1L, PaymentStatus.APPROVED, pageable))
                    .thenReturn(new PageImpl<>(List.of(buildPayment(4L, user, PaymentStatus.APPROVED))));

            Page<PaymentResponse> result = service.getAll(1L, PaymentStatus.APPROVED, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).status()).isEqualTo(PaymentStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("approvePayment()")
    class Approve {

        @Test
        @DisplayName("Transiciona PENDING a APPROVED correctamente")
        void approvesPendingPayment() {
            Payment payment = buildPayment(1L, user, PaymentStatus.PENDING);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = service.approvePayment(1L);

            assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        }

        @Test
        @DisplayName("Lanza BadRequestException si el pago ya está APPROVED")
        void throwsWhenAlreadyApproved() {
            Payment payment = buildPayment(1L, user, PaymentStatus.APPROVED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> service.approvePayment(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("APPROVED");
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza BadRequestException si el pago ya está REJECTED")
        void throwsWhenAlreadyRejected() {
            Payment payment = buildPayment(1L, user, PaymentStatus.REJECTED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> service.approvePayment(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("REJECTED");
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el pago no existe")
        void throwsWhenNotFound() {
            when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approvePayment(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("rejectPayment()")
    class Reject {

        @Test
        @DisplayName("Transiciona PENDING a REJECTED correctamente")
        void rejectsPendingPayment() {
            Payment payment = buildPayment(1L, user, PaymentStatus.PENDING);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = service.rejectPayment(1L);

            assertThat(response.status()).isEqualTo(PaymentStatus.REJECTED);
        }

        @Test
        @DisplayName("Lanza BadRequestException si el pago ya está APPROVED")
        void throwsWhenAlreadyApproved() {
            Payment payment = buildPayment(1L, user, PaymentStatus.APPROVED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> service.rejectPayment(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("APPROVED");
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza BadRequestException si el pago ya está REJECTED")
        void throwsWhenAlreadyRejected() {
            Payment payment = buildPayment(1L, user, PaymentStatus.REJECTED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> service.rejectPayment(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("REJECTED");
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el pago no existe")
        void throwsWhenNotFound() {
            when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.rejectPayment(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
