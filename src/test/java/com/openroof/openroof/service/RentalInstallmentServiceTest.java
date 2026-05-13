package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.RentalInstallmentMapper;
import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.repository.RentalInstallmentRepository;
import com.openroof.openroof.security.LeaseSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalInstallmentServiceTest {

    @Mock private RentalInstallmentRepository installmentRepository;
    @Mock private RentalInstallmentMapper installmentMapper;
    @Mock private LeaseSecurity leaseSecurity;

    private RentalInstallmentService service;

    private static final Long LEASE_ID = 10L;
    private static final Long USER_ID = 1L;
    private static final Long INSTALLMENT_ID = 50L;

    private Lease lease;
    private RentalInstallment installment;
    private RentalInstallmentResponse installmentResponse;

    @BeforeEach
    void setUp() {
        service = new RentalInstallmentService(installmentRepository, installmentMapper, leaseSecurity);

        lease = Lease.builder()
                .leaseType(LeaseType.FIXED_TERM)
                .status(LeaseStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .monthlyRent(new BigDecimal("1000.00"))
                .build();
        lease.setId(LEASE_ID);

        installment = RentalInstallment.builder()
                .lease(lease)
                .installmentNumber(1)
                .dueDate(LocalDate.now().plusMonths(1))
                .baseRent(new BigDecimal("1000.00"))
                .totalAmount(new BigDecimal("1000.00"))
                .status(InstallmentStatus.PENDING)
                .build();
        installment.setId(INSTALLMENT_ID);

        installmentResponse = new RentalInstallmentResponse(
                INSTALLMENT_ID, LEASE_ID, 1,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                LocalDate.now().plusMonths(1), null,
                InstallmentStatus.PENDING, null, LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listByLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listByLease()")
    class ListByLease {

        private final Pageable pageable = PageRequest.of(0, 12);

        @Test
        @DisplayName("Verifica acceso y retorna página mapeada de cuotas")
        void authorizedUser_returnsMappedPage() {
            Page<RentalInstallment> repoPage = new PageImpl<>(List.of(installment));
            when(installmentRepository.findByLeaseIdOrderByDueDateDesc(LEASE_ID, pageable))
                    .thenReturn(repoPage);
            when(installmentMapper.toResponse(installment)).thenReturn(installmentResponse);

            Page<RentalInstallmentResponse> result = service.listByLease(LEASE_ID, USER_ID, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(INSTALLMENT_ID);
            assertThat(result.getContent().get(0).leaseId()).isEqualTo(LEASE_ID);
            verify(leaseSecurity).assertInstallmentAccess(USER_ID, LEASE_ID);
            verify(installmentRepository).findByLeaseIdOrderByDueDateDesc(LEASE_ID, pageable);
        }

        @Test
        @DisplayName("Retorna página vacía cuando el lease no tiene cuotas")
        void noInstallments_returnsEmptyPage() {
            when(installmentRepository.findByLeaseIdOrderByDueDateDesc(LEASE_ID, pageable))
                    .thenReturn(Page.empty(pageable));

            Page<RentalInstallmentResponse> result = service.listByLease(LEASE_ID, USER_ID, pageable);

            assertThat(result.getTotalElements()).isEqualTo(0);
            verifyNoInteractions(installmentMapper);
        }

        @Test
        @DisplayName("Propaga AccessDeniedException cuando el usuario no tiene acceso al lease")
        void accessDenied_propagatesException() {
            doThrow(new AccessDeniedException("sin acceso"))
                    .when(leaseSecurity).assertInstallmentAccess(USER_ID, LEASE_ID);

            assertThatThrownBy(() -> service.listByLease(LEASE_ID, USER_ID, pageable))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("sin acceso");
            verifyNoInteractions(installmentRepository);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getById
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Retorna la cuota mapeada para un usuario autorizado")
        void authorizedUser_returnsMappedResponse() {
            when(installmentRepository.findById(INSTALLMENT_ID)).thenReturn(Optional.of(installment));
            when(installmentMapper.toResponse(installment)).thenReturn(installmentResponse);

            RentalInstallmentResponse result = service.getById(INSTALLMENT_ID, USER_ID);

            assertThat(result.id()).isEqualTo(INSTALLMENT_ID);
            assertThat(result.leaseId()).isEqualTo(LEASE_ID);
            verify(leaseSecurity).assertInstallmentAccess(USER_ID, LEASE_ID);
        }

        @Test
        @DisplayName("Verifica acceso usando el leaseId de la cuota encontrada")
        void usesLeaseIdFromInstallment_forAccessCheck() {
            when(installmentRepository.findById(INSTALLMENT_ID)).thenReturn(Optional.of(installment));
            when(installmentMapper.toResponse(installment)).thenReturn(installmentResponse);

            service.getById(INSTALLMENT_ID, USER_ID);

            verify(leaseSecurity).assertInstallmentAccess(USER_ID, LEASE_ID);
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si la cuota no existe")
        void installmentNotFound_throwsResourceNotFoundException() {
            when(installmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(999L, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(leaseSecurity);
        }

        @Test
        @DisplayName("Propaga AccessDeniedException cuando el usuario no tiene acceso al lease")
        void accessDenied_propagatesException() {
            when(installmentRepository.findById(INSTALLMENT_ID)).thenReturn(Optional.of(installment));
            doThrow(new AccessDeniedException("sin acceso"))
                    .when(leaseSecurity).assertInstallmentAccess(USER_ID, LEASE_ID);

            assertThatThrownBy(() -> service.getById(INSTALLMENT_ID, USER_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}
