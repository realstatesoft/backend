package com.openroof.openroof.service;

import com.openroof.openroof.dto.subscription.SubscriptionPlanResponse;
import com.openroof.openroof.dto.subscription.SubscriptionResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.enums.PaymentType;
import com.openroof.openroof.model.enums.SubscriptionStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.subscription.Subscription;
import com.openroof.openroof.model.subscription.SubscriptionPlan;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PaymentRepository;
import com.openroof.openroof.repository.SubscriptionPlanRepository;
import com.openroof.openroof.repository.SubscriptionRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SubscriptionServiceIntegrationTest {

    @Autowired SubscriptionService subscriptionService;
    @Autowired SubscriptionPlanService subscriptionPlanService;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired SubscriptionPlanRepository planRepository;
    @Autowired UserRepository userRepository;
    @Autowired PaymentRepository paymentRepository;

    private User regularUser;
    private User adminUser;
    private SubscriptionPlan activePlan;
    private SubscriptionPlan inactivePlan;
    private Payment payment;

    @BeforeEach
    void setUp() {
        regularUser = userRepository.save(User.builder()
                .email("user@integration.test")
                .passwordHash("hash")
                .name("Regular User")
                .role(UserRole.USER)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin@integration.test")
                .passwordHash("hash")
                .name("Admin User")
                .role(UserRole.ADMIN)
                .build());

        activePlan = planRepository.save(SubscriptionPlan.builder()
                .name("Plan Básico")
                .description("Plan mensual básico")
                .price(new BigDecimal("150000.00"))
                .durationMonths(1)
                .active(true)
                .build());

        inactivePlan = planRepository.save(SubscriptionPlan.builder()
                .name("Plan Antiguo")
                .description("Plan deprecado")
                .price(new BigDecimal("100000.00"))
                .durationMonths(1)
                .active(false)
                .build());

        payment = paymentRepository.save(Payment.builder()
                .user(regularUser)
                .type(PaymentType.SUBSCRIPTION)
                .status(PaymentStatus.APPROVED)
                .transactionCode(UUID.randomUUID().toString())
                .amount(new BigDecimal("150000.00"))
                .concept("Suscripción mensual")
                .build());
    }

    @Nested
    @DisplayName("SubscriptionPlanService - integración")
    class PlanServiceIntegration {

        @Test
        @DisplayName("getAll(true) devuelve solo planes activos persistidos en BD")
        void getAll_activeOnly_returnsPersistedActivePlans() {
            Page<SubscriptionPlanResponse> result = subscriptionPlanService.getAll(true, PageRequest.of(0, 10));

            assertThat(result.getContent())
                    .extracting(SubscriptionPlanResponse::name)
                    .contains("Plan Básico")
                    .doesNotContain("Plan Antiguo");
        }

        @Test
        @DisplayName("getAll(null) devuelve todos los planes activos e inactivos")
        void getAll_noFilter_returnsAll() {
            Page<SubscriptionPlanResponse> result = subscriptionPlanService.getAll(null, PageRequest.of(0, 10));

            assertThat(result.getContent())
                    .extracting(SubscriptionPlanResponse::name)
                    .contains("Plan Básico", "Plan Antiguo");
        }

        @Test
        @DisplayName("getActiveById() devuelve plan activo y lanza excepción para inactivo")
        void getActiveById_activeReturnsData_inactiveThrows() {
            SubscriptionPlanResponse res = subscriptionPlanService.getActiveById(activePlan.getId());
            assertThat(res.name()).isEqualTo("Plan Básico");
            assertThat(res.active()).isTrue();

            assertThatThrownBy(() -> subscriptionPlanService.getActiveById(inactivePlan.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("create() persiste el plan y es recuperable después")
        void create_persistsPlan() {
            var request = new com.openroof.openroof.dto.subscription.SubscriptionPlanRequest(
                    "Plan Nuevo", "Descripción del plan nuevo",
                    new BigDecimal("200000.00"), 3, true);

            SubscriptionPlanResponse created = subscriptionPlanService.create(request);

            assertThat(created.id()).isNotNull();
            assertThat(created.name()).isEqualTo("Plan Nuevo");

            SubscriptionPlanResponse fetched = subscriptionPlanService.getById(created.id());
            assertThat(fetched.name()).isEqualTo("Plan Nuevo");
            assertThat(fetched.durationMonths()).isEqualTo(3);
        }

        @Test
        @DisplayName("deactivate() persiste active=false en BD")
        void deactivate_persistsInactiveState() {
            subscriptionPlanService.deactivate(activePlan.getId());

            SubscriptionPlan updated = planRepository.findById(activePlan.getId()).orElseThrow();
            assertThat(updated.getActive()).isFalse();
        }

        @Test
        @DisplayName("delete() elimina el plan si no tiene suscripciones activas")
        void delete_noActiveSubs_removesPlan() {
            Long planId = inactivePlan.getId();
            subscriptionPlanService.delete(planId);

            assertThat(planRepository.findById(planId)).isEmpty();
        }

        @Test
        @DisplayName("delete() lanza BadRequestException si el plan tiene suscripciones activas")
        void delete_withActiveSub_throwsBadRequest() {
            subscriptionRepository.save(Subscription.builder()
                    .user(regularUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startsAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMonths(1))
                    .build());

            assertThatThrownBy(() -> subscriptionPlanService.delete(activePlan.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("activas");
        }
    }

    @Nested
    @DisplayName("SubscriptionService - integración")
    class SubscriptionServiceIntegration {

        @Test
        @DisplayName("activateSubscription() crea suscripción activa con fecha de expiración correcta")
        void activateSubscription_createsActiveSubscription() {
            LocalDateTime before = LocalDateTime.now();

            SubscriptionResponse res = subscriptionService.activateSubscription(
                    regularUser.getId(), payment.getId(), activePlan.getId());

            assertThat(res.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(res.userId()).isEqualTo(regularUser.getId());
            assertThat(res.plan().id()).isEqualTo(activePlan.getId());
            assertThat(res.startsAt()).isAfterOrEqualTo(before);
            assertThat(res.expiresAt()).isAfterOrEqualTo(res.startsAt().plusMonths(1).minusSeconds(1));
        }

        @Test
        @DisplayName("getMySubscriptions() devuelve solo las suscripciones del usuario solicitante")
        void getMySubscriptions_returnsOnlyUserSubscriptions() {
            User otherUser = userRepository.save(User.builder()
                    .email("other@integration.test")
                    .passwordHash("hash")
                    .name("Other User")
                    .role(UserRole.USER)
                    .build());

            subscriptionRepository.save(Subscription.builder()
                    .user(regularUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startsAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMonths(1))
                    .build());

            subscriptionRepository.save(Subscription.builder()
                    .user(otherUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startsAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMonths(1))
                    .build());

            Page<SubscriptionResponse> result = subscriptionService.getMySubscriptions(
                    regularUser.getEmail(), PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).userId()).isEqualTo(regularUser.getId());
        }

        @Test
        @DisplayName("getMyActiveSubscription() devuelve la suscripción activa del usuario")
        void getMyActiveSubscription_returnsActiveSub() {
            subscriptionRepository.save(Subscription.builder()
                    .user(regularUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startsAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMonths(1))
                    .build());

            Optional<SubscriptionResponse> result = subscriptionService.getMyActiveSubscription(regularUser.getEmail());

            assertThat(result).isPresent();
            assertThat(result.get().status()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("getMyActiveSubscription() devuelve vacío cuando no hay suscripción activa")
        void getMyActiveSubscription_noActive_returnsEmpty() {
            subscriptionRepository.save(Subscription.builder()
                    .user(regularUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.EXPIRED)
                    .startsAt(LocalDateTime.now().minusMonths(2))
                    .expiresAt(LocalDateTime.now().minusMonths(1))
                    .build());

            Optional<SubscriptionResponse> result = subscriptionService.getMyActiveSubscription(regularUser.getEmail());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("cancelSubscription() persiste CANCELLED y registra cancelledAt")
        void cancelSubscription_persistsCancelledState() {
            Subscription sub = subscriptionRepository.save(Subscription.builder()
                    .user(regularUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startsAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMonths(1))
                    .build());

            SubscriptionResponse res = subscriptionService.cancelSubscription(sub.getId(), regularUser.getEmail());

            assertThat(res.status()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(res.cancelledAt()).isNotNull();

            Subscription persisted = subscriptionRepository.findById(sub.getId()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("cancelSubscription() lanza ForbiddenException si el usuario no es propietario ni admin")
        void cancelSubscription_foreignUser_throwsForbidden() {
            User otherUser = userRepository.save(User.builder()
                    .email("other2@integration.test")
                    .passwordHash("hash")
                    .name("Other User 2")
                    .role(UserRole.USER)
                    .build());

            Subscription sub = subscriptionRepository.save(Subscription.builder()
                    .user(regularUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startsAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMonths(1))
                    .build());

            assertThatThrownBy(() -> subscriptionService.cancelSubscription(sub.getId(), otherUser.getEmail()))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("cancelSubscription() lanza BadRequestException si la suscripción ya no está activa")
        void cancelSubscription_notActive_throwsBadRequest() {
            Subscription sub = subscriptionRepository.save(Subscription.builder()
                    .user(regularUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.CANCELLED)
                    .startsAt(LocalDateTime.now().minusDays(10))
                    .expiresAt(LocalDateTime.now().plusDays(20))
                    .build());

            assertThatThrownBy(() -> subscriptionService.cancelSubscription(sub.getId(), regularUser.getEmail()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("deactivateExpired() marca como EXPIRED las suscripciones vencidas")
        void deactivateExpired_marksExpiredSubscriptions() {
            subscriptionRepository.save(Subscription.builder()
                    .user(regularUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startsAt(LocalDateTime.now().minusMonths(2))
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .build());

            int count = subscriptionService.deactivateExpired();

            assertThat(count).isGreaterThanOrEqualTo(1);

            Subscription updated = subscriptionRepository.findAll().stream()
                    .filter(s -> s.getUser().getId().equals(regularUser.getId()))
                    .findFirst().orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        }

        @Test
        @DisplayName("deactivateExpired() no afecta suscripciones que aún no han vencido")
        void deactivateExpired_doesNotAffectFutureSubs() {
            Subscription future = subscriptionRepository.save(Subscription.builder()
                    .user(regularUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startsAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMonths(1))
                    .build());

            subscriptionService.deactivateExpired();

            Subscription reloaded = subscriptionRepository.findById(future.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }
    }
}
