package com.openroof.openroof.service;

import com.openroof.openroof.dto.subscription.SubscriptionPlanResponse;
import com.openroof.openroof.dto.subscription.SubscriptionResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ConflictException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.SubscriptionStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.subscription.Subscription;
import com.openroof.openroof.model.subscription.SubscriptionPlan;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PaymentRepository;
import com.openroof.openroof.repository.SubscriptionRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private SubscriptionPlanService subscriptionPlanService;

    @InjectMocks
    private SubscriptionService service;

    private User user(Long id, String email, UserRole role) {
        User u = User.builder()
                .email(email)
                .passwordHash("hash")
                .name("Test User")
                .role(role)
                .build();
        u.setId(id);
        return u;
    }

    private SubscriptionPlan plan(Long id) {
        SubscriptionPlan p = SubscriptionPlan.builder()
                .name("Básico")
                .price(new BigDecimal("150000.00"))
                .durationMonths(1)
                .active(true)
                .build();
        p.setId(id);
        return p;
    }

    private SubscriptionPlanResponse planResponse(Long id) {
        return new SubscriptionPlanResponse(id, "Básico", "Desc",
                new BigDecimal("150000.00"), 1, true, LocalDateTime.now(), LocalDateTime.now());
    }

    private Payment payment(Long id) {
        Payment p = new Payment();
        p.setId(id);
        return p;
    }

    private Subscription subscription(Long id, User user, SubscriptionPlan plan, SubscriptionStatus status) {
        Subscription s = Subscription.builder()
                .user(user)
                .plan(plan)
                .status(status)
                .startsAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();
        s.setId(id);
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("Sin filtro devuelve todas las suscripciones paginadas")
        void noFilter_returnsAll() {
            User u = user(1L, "u@test.com", UserRole.USER);
            Subscription sub = subscription(1L, u, plan(1L), SubscriptionStatus.ACTIVE);
            Pageable pageable = PageRequest.of(0, 10);

            when(subscriptionRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sub)));
            when(subscriptionPlanService.toResponse(any())).thenReturn(planResponse(1L));

            var result = service.getAll(null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Con filtro de estado devuelve solo las del estado indicado")
        void withStatusFilter_returnsFiltered() {
            User u = user(1L, "u@test.com", UserRole.USER);
            Subscription sub = subscription(1L, u, plan(1L), SubscriptionStatus.ACTIVE);
            Pageable pageable = PageRequest.of(0, 10);

            when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE, pageable))
                    .thenReturn(new PageImpl<>(List.of(sub)));
            when(subscriptionPlanService.toResponse(any())).thenReturn(planResponse(1L));

            var result = service.getAll(SubscriptionStatus.ACTIVE, pageable);

            assertThat(result.getContent().get(0).status()).isEqualTo(SubscriptionStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Suscripción existente devuelve su response correctamente")
        void found_returnsResponse() {
            User u = user(1L, "u@test.com", UserRole.USER);
            Subscription sub = subscription(1L, u, plan(1L), SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
            when(subscriptionPlanService.toResponse(any())).thenReturn(planResponse(1L));

            SubscriptionResponse res = service.getById(1L);

            assertThat(res.id()).isEqualTo(1L);
            assertThat(res.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Suscripción inexistente lanza ResourceNotFoundException")
        void notFound_throwsResourceNotFound() {
            when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMySubscriptions()")
    class GetMySubscriptions {

        @Test
        @DisplayName("Devuelve las suscripciones del usuario autenticado")
        void returnsUserSubscriptions() {
            User u = user(1L, "user@test.com", UserRole.USER);
            Subscription sub = subscription(1L, u, plan(1L), SubscriptionStatus.ACTIVE);
            Pageable pageable = PageRequest.of(0, 10);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(u));
            when(subscriptionRepository.findByUser_Id(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(sub)));
            when(subscriptionPlanService.toResponse(any())).thenReturn(planResponse(1L));

            var result = service.getMySubscriptions("user@test.com", pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Usuario inexistente lanza ResourceNotFoundException")
        void userNotFound_throwsNotFound() {
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMySubscriptions("ghost@test.com", PageRequest.of(0, 10)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMyActiveSubscription()")
    class GetMyActiveSubscription {

        @Test
        @DisplayName("Devuelve la suscripción activa del usuario")
        void withActiveSub_returnsPresent() {
            User u = user(1L, "user@test.com", UserRole.USER);
            Subscription sub = subscription(1L, u, plan(1L), SubscriptionStatus.ACTIVE);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(u));
            when(subscriptionRepository.findFirstByUser_IdAndStatusOrderByExpiresAtDesc(1L, SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.of(sub));
            when(subscriptionPlanService.toResponse(any())).thenReturn(planResponse(1L));

            Optional<SubscriptionResponse> result = service.getMyActiveSubscription("user@test.com");

            assertThat(result).isPresent();
            assertThat(result.get().status()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Sin suscripción activa devuelve vacío")
        void noActiveSub_returnsEmpty() {
            User u = user(1L, "user@test.com", UserRole.USER);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(u));
            when(subscriptionRepository.findFirstByUser_IdAndStatusOrderByExpiresAtDesc(1L, SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            Optional<SubscriptionResponse> result = service.getMyActiveSubscription("user@test.com");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("activateSubscription()")
    class ActivateSubscription {

        @Test
        @DisplayName("Activa la suscripción y la persiste correctamente")
        void validData_createsSubscription() {
            User u = user(1L, "u@test.com", UserRole.USER);
            Payment p = payment(10L);
            SubscriptionPlan pl = plan(2L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(u));
            when(paymentRepository.findById(10L)).thenReturn(Optional.of(p));
            when(subscriptionPlanService.getPlanOrThrow(2L)).thenReturn(pl);
            when(subscriptionRepository.save(any())).thenAnswer(inv -> {
                Subscription s = inv.getArgument(0);
                s.setId(99L);
                s.setCreatedAt(LocalDateTime.now());
                s.setUpdatedAt(LocalDateTime.now());
                return s;
            });
            when(subscriptionPlanService.toResponse(pl)).thenReturn(planResponse(2L));

            SubscriptionResponse res = service.activateSubscription(1L, 10L, 2L);

            assertThat(res.id()).isEqualTo(99L);
            assertThat(res.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Suscripción activa duplicada convierte DataIntegrityViolation en ConflictException")
        void duplicateActive_throwsConflict() {
            User u = user(1L, "u@test.com", UserRole.USER);
            Payment p = payment(10L);
            SubscriptionPlan pl = plan(2L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(u));
            when(paymentRepository.findById(10L)).thenReturn(Optional.of(p));
            when(subscriptionPlanService.getPlanOrThrow(2L)).thenReturn(pl);
            when(subscriptionRepository.save(any())).thenThrow(new DataIntegrityViolationException("unique"));

            assertThatThrownBy(() -> service.activateSubscription(1L, 10L, 2L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("activa");
        }

        @Test
        @DisplayName("Usuario inexistente lanza ResourceNotFoundException")
        void userNotFound_throwsNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activateSubscription(99L, 10L, 2L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancelSubscription()")
    class CancelSubscription {

        @Test
        @DisplayName("El propietario puede cancelar su suscripción activa")
        void ownerCancels_success() {
            User owner = user(1L, "owner@test.com", UserRole.USER);
            Subscription sub = subscription(1L, owner, plan(1L), SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(subscriptionPlanService.toResponse(any())).thenReturn(planResponse(1L));

            SubscriptionResponse res = service.cancelSubscription(1L, "owner@test.com");

            assertThat(res.status()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("ADMIN puede cancelar la suscripción de otro usuario")
        void adminCancels_success() {
            User owner = user(1L, "owner@test.com", UserRole.USER);
            User admin = user(2L, "admin@test.com", UserRole.ADMIN);
            Subscription sub = subscription(1L, owner, plan(1L), SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(subscriptionPlanService.toResponse(any())).thenReturn(planResponse(1L));

            SubscriptionResponse res = service.cancelSubscription(1L, "admin@test.com");

            assertThat(res.status()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("Tercero sin permisos lanza ForbiddenException")
        void thirdParty_throwsForbidden() {
            User owner = user(1L, "owner@test.com", UserRole.USER);
            User other = user(2L, "other@test.com", UserRole.USER);
            Subscription sub = subscription(1L, owner, plan(1L), SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
            when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.cancelSubscription(1L, "other@test.com"))
                    .isInstanceOf(ForbiddenException.class);
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Suscripción ya no activa lanza BadRequestException")
        void alreadyCancelled_throwsBadRequest() {
            User owner = user(1L, "owner@test.com", UserRole.USER);
            Subscription sub = subscription(1L, owner, plan(1L), SubscriptionStatus.CANCELLED);

            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));

            assertThatThrownBy(() -> service.cancelSubscription(1L, "owner@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ACTIVE");
            verify(subscriptionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivateExpired()")
    class DeactivateExpired {

        @Test
        @DisplayName("Llama al repositorio y devuelve el conteo de suscripciones expiradas")
        void callsRepositoryAndReturnsCount() {
            when(subscriptionRepository.expireOlderThan(
                    eq(SubscriptionStatus.ACTIVE),
                    eq(SubscriptionStatus.EXPIRED),
                    any(LocalDateTime.class))).thenReturn(3);

            int count = service.deactivateExpired();

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Sin suscripciones expiradas devuelve 0")
        void noExpired_returnsZero() {
            when(subscriptionRepository.expireOlderThan(any(), any(), any())).thenReturn(0);

            assertThat(service.deactivateExpired()).isEqualTo(0);
        }
    }
}
