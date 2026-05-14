package com.openroof.openroof.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TenantDashboardResponse(

        // Estado general del tenant
        TenantStatus status,
        String statusMessage,

        // Leases activos
        java.util.List<ActiveLeaseInfo> activeLeases,

        // Próxima cuota
        NextInstallmentInfo nextInstallment,

        // Balance pendiente total
        BigDecimal pendingBalance,

        // Tickets de mantenimiento activos
        long openMaintenanceTickets,

        // Mensajes no leídos
        long unreadMessages,

        // Último pago
        LastPaymentInfo lastPayment,

        // Resumen financiero
        BigDecimal totalPaidLastYear,

        // Listas para el resumen
        java.util.List<NextInstallmentInfo> recentInstallments,
        java.util.List<MaintenanceTicketInfo> recentMaintenanceTickets
) {
    public record MaintenanceTicketInfo(
        Long id,
        String title,
        String category,
        String status,
        LocalDate createdAt
    ) {}

        public record ActiveLeaseInfo(
                Long leaseId,
                String propertyTitle,
                String propertyAddress,
                String landlordName,
                String landlordEmail,
                String landlordPhone,
                LocalDate startDate,
                LocalDate endDate,
                long daysRemaining,
                BigDecimal monthlyRent,
                String currency
        ) {}

        public record NextInstallmentInfo(
                Long installmentId,
                int installmentNumber,
                BigDecimal totalAmount,
                BigDecimal paidAmount,
                BigDecimal balance,
                LocalDate dueDate,
                String status,
                long daysUntilDue
        ) {}

        public record LastPaymentInfo(
                Long paymentId,
                BigDecimal amount,
                String concept,
                LocalDateTime paymentDate,
                String transactionCode
        ) {}

        public enum TenantStatus {
                ACTIVE,
                INACTIVE
        }
}
