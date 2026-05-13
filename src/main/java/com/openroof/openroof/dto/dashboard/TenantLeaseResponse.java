package com.openroof.openroof.dto.dashboard;

import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.DepositStatus;
import com.openroof.openroof.model.enums.LateFeeType;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TenantLeaseResponse(

        Long id,
        String statusMessage,

        // ─── Propiedad ────────────────────────────────────────────
        Long propertyId,
        String propertyTitle,
        String propertyAddress,

        // ─── Landlord / Property Manager ──────────────────────────
        LandlordContact landlord,

        // ─── Detalles del Lease ───────────────────────────────────
        LeaseStatus status,
        LocalDate startDate,
        LocalDate endDate,
        LeaseType leaseType,
        BigDecimal monthlyRent,
        String currency,
        BillingFrequency billingFrequency,
        Integer dueDay,
        Integer gracePeriodDays,
        LateFeeType lateFeeType,
        BigDecimal lateFeeValue,
        BigDecimal maxLateFeeCap,
        BigDecimal securityDeposit,
        DepositStatus depositStatus,
        LocalDate moveInDate,
        Boolean autoRenew,
        Integer renewalNoticeDays,
        long daysRemaining,

        // ─── Información Adicional ──────────────────────────────
        String pets,
        String includedServices,
        String emergencyContact,

        // ─── Documentos ──────────────────────────────────────────
        List<LeaseDocument> documents,

        // ─── Firma electrónica ───────────────────────────────────
        boolean signed,
        LocalDateTime signedByLandlordAt,
        LocalDateTime signedByTenantAt,

        // ─── Auditoría ───────────────────────────────────────────
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
        public record LandlordContact(
                Long userId,
                String name,
                String email,
                String phone,
                String avatarUrl
        ) {}

        public record LeaseDocument(
                String type,
                String fileName,
                String fileUrl,
                boolean isSigned
        ) {}
}
