package com.openroof.openroof.service;

import com.openroof.openroof.dto.subscription.SubscriptionPlanRequest;
import com.openroof.openroof.dto.subscription.SubscriptionPlanResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ConflictException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.SubscriptionStatus;
import com.openroof.openroof.model.subscription.SubscriptionPlan;
import com.openroof.openroof.repository.SubscriptionPlanRepository;
import com.openroof.openroof.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanServiceTest {

    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionPlanService service;

    private SubscriptionPlan plan(Long id, String name, boolean active) {
        SubscriptionPlan p = SubscriptionPlan.builder()
                .name(name)
                .description("Descripción de " + name)
                .price(new BigDecimal("150000.00"))
                .durationMonths(1)
                .active(active)
                .build();
        p.setId(id);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    private SubscriptionPlanRequest request(String name) {
        return new SubscriptionPlanRequest(name, "Descripción", new BigDecimal("150000.00"), 1, true);
    }

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("Sin filtro devuelve todos los planes paginados")
        void noFilter_returnsAll() {
            Pageable pageable = PageRequest.of(0, 10);
            when(planRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(plan(1L, "Básico", true), plan(2L, "Premium", false))));

            var result = service.getAll(null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("active=true filtra solo planes activos")
        void activeTrue_returnsOnlyActive() {
            Pageable pageable = PageRequest.of(0, 10);
            when(planRepository.findByActive(true, pageable))
                    .thenReturn(new PageImpl<>(List.of(plan(1L, "Básico", true))));

            var result = service.getAll(true, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).active()).isTrue();
        }

        @Test
        @DisplayName("active=false filtra solo planes inactivos")
        void activeFalse_returnsOnlyInactive() {
            Pageable pageable = PageRequest.of(0, 10);
            when(planRepository.findByActive(false, pageable))
                    .thenReturn(new PageImpl<>(List.of(plan(2L, "Antiguo", false))));

            var result = service.getAll(false, pageable);

            assertThat(result.getContent().get(0).active()).isFalse();
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Plan existente devuelve su response correctamente mapeado")
        void found_returnsResponse() {
            when(planRepository.findById(1L)).thenReturn(Optional.of(plan(1L, "Básico", true)));

            SubscriptionPlanResponse res = service.getById(1L);

            assertThat(res.id()).isEqualTo(1L);
            assertThat(res.name()).isEqualTo("Básico");
            assertThat(res.active()).isTrue();
        }

        @Test
        @DisplayName("Plan inexistente lanza ResourceNotFoundException")
        void notFound_throwsResourceNotFound() {
            when(planRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan");
        }
    }

    @Nested
    @DisplayName("getActiveById()")
    class GetActiveById {

        @Test
        @DisplayName("Plan activo devuelve su response")
        void activePlan_returnsResponse() {
            when(planRepository.findById(1L)).thenReturn(Optional.of(plan(1L, "Básico", true)));

            SubscriptionPlanResponse res = service.getActiveById(1L);

            assertThat(res.id()).isEqualTo(1L);
            assertThat(res.active()).isTrue();
        }

        @Test
        @DisplayName("Plan inactivo lanza ResourceNotFoundException")
        void inactivePlan_throwsNotFound() {
            when(planRepository.findById(2L)).thenReturn(Optional.of(plan(2L, "Antiguo", false)));

            assertThatThrownBy(() -> service.getActiveById(2L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Plan inexistente lanza ResourceNotFoundException")
        void notFound_throwsResourceNotFound() {
            when(planRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getActiveById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Crea un plan con los datos correctos y lo persiste")
        void validRequest_createsPlan() {
            when(planRepository.existsByName("Premium")).thenReturn(false);
            when(planRepository.save(any())).thenAnswer(inv -> {
                SubscriptionPlan p = inv.getArgument(0);
                p.setId(1L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            SubscriptionPlanResponse res = service.create(request("Premium"));

            assertThat(res.id()).isEqualTo(1L);
            assertThat(res.name()).isEqualTo("Premium");
            assertThat(res.active()).isTrue();
        }

        @Test
        @DisplayName("Nombre duplicado lanza ConflictException")
        void duplicateName_throwsConflict() {
            when(planRepository.existsByName("Básico")).thenReturn(true);

            assertThatThrownBy(() -> service.create(request("Básico")))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Básico");
            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nombre con espacios extremos se normaliza antes del check de duplicados")
        void nameTrimmedBeforeDuplicateCheck() {
            when(planRepository.existsByName("Premium")).thenReturn(false);
            when(planRepository.save(any())).thenAnswer(inv -> {
                SubscriptionPlan p = inv.getArgument(0);
                p.setId(1L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            SubscriptionPlanRequest req = new SubscriptionPlanRequest(
                    "  Premium  ", "Descripción", new BigDecimal("150000.00"), 1, true);
            SubscriptionPlanResponse res = service.create(req);

            assertThat(res.name()).isEqualTo("Premium");
            verify(planRepository).existsByName("Premium");
        }

        @Test
        @DisplayName("active=null en el request se guarda como true por defecto")
        void nullActiveSavedAsTrue() {
            SubscriptionPlanRequest req = new SubscriptionPlanRequest(
                    "Básico", "Descripción", new BigDecimal("100000.00"), 1, null);
            when(planRepository.existsByName("Básico")).thenReturn(false);
            when(planRepository.save(any())).thenAnswer(inv -> {
                SubscriptionPlan p = inv.getArgument(0);
                p.setId(1L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            SubscriptionPlanResponse res = service.create(req);

            assertThat(res.active()).isTrue();
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Actualiza los campos del plan correctamente")
        void validUpdate_persistsChanges() {
            SubscriptionPlan existing = plan(1L, "Básico", true);
            when(planRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(planRepository.existsByNameAndIdNot("Nuevo Nombre", 1L)).thenReturn(false);
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubscriptionPlanRequest req = new SubscriptionPlanRequest(
                    "Nuevo Nombre", "Nueva descripción", new BigDecimal("200000.00"), 3, true);
            SubscriptionPlanResponse res = service.update(1L, req);

            assertThat(res.name()).isEqualTo("Nuevo Nombre");
            assertThat(res.durationMonths()).isEqualTo(3);
            assertThat(res.price()).isEqualByComparingTo("200000.00");
        }

        @Test
        @DisplayName("Plan inexistente lanza ResourceNotFoundException")
        void planNotFound_throwsNotFound() {
            when(planRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, request("Premium")))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nombre ya usado por otro plan lanza ConflictException")
        void duplicateNameOtherPlan_throwsConflict() {
            when(planRepository.findById(1L)).thenReturn(Optional.of(plan(1L, "Básico", true)));
            when(planRepository.existsByNameAndIdNot("Premium", 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.update(1L, request("Premium")))
                    .isInstanceOf(ConflictException.class);
            verify(planRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        @Test
        @DisplayName("Desactiva el plan y persiste active=false")
        void deactivatesPlan() {
            SubscriptionPlan existing = plan(1L, "Premium", true);
            when(planRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubscriptionPlanResponse res = service.deactivate(1L);

            assertThat(res.active()).isFalse();
            verify(planRepository).save(existing);
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Elimina el plan si no tiene suscripciones activas")
        void noActiveSubs_deletesPlan() {
            SubscriptionPlan existing = plan(1L, "Básico", false);
            when(planRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(subscriptionRepository.existsByPlan_IdAndStatusIn(
                    eq(1L), eq(List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING))))
                    .thenReturn(false);

            service.delete(1L);

            verify(planRepository).delete(existing);
        }

        @Test
        @DisplayName("Plan con suscripciones activas lanza BadRequestException")
        void withActiveSubs_throwsBadRequest() {
            SubscriptionPlan existing = plan(1L, "Premium", true);
            when(planRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(subscriptionRepository.existsByPlan_IdAndStatusIn(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> service.delete(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("activas");
            verify(planRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Plan inexistente lanza ResourceNotFoundException")
        void planNotFound_throwsNotFound() {
            when(planRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
