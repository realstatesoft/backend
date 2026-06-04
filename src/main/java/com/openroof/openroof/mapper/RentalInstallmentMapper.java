package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.rental.LeasePaymentResponse;
import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.model.rental.LeasePayment;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.repository.LeasePaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RentalInstallmentMapper {

    private final LeasePaymentRepository leasePaymentRepository;

    public RentalInstallmentResponse toResponse(RentalInstallment installment) {
        BigDecimal baseRent = installment.getBaseRent() != null ? installment.getBaseRent() : BigDecimal.ZERO;
        BigDecimal lateFee  = installment.getLateFee()  != null ? installment.getLateFee()  : BigDecimal.ZERO;

        String currency = null;
        if (installment.getLease() != null && installment.getLease().getCurrency() != null) {
            currency = installment.getLease().getCurrency();
        }

        List<LeasePaymentResponse> payments = null;
        if (installment.getId() != null) {
            List<LeasePayment> paymentEntities = leasePaymentRepository.findByInstallmentIdOrderByPaidAtDesc(installment.getId());
            payments = paymentEntities.stream()
                    .map(this::toPaymentResponse)
                    .collect(Collectors.toList());
        }

        return new RentalInstallmentResponse(
                installment.getId(),
                installment.getLease() != null ? installment.getLease().getId() : null,
                installment.getInstallmentNumber(),
                baseRent,
                lateFee,
                baseRent.add(lateFee),
                installment.getDueDate(),
                installment.getPaidDate(),
                installment.getStatus(),
                installment.getNotes(),
                installment.getCreatedAt(),
                currency,
                payments
        );
    }

    private LeasePaymentResponse toPaymentResponse(LeasePayment payment) {
        return new LeasePaymentResponse(
                payment.getId(),
                payment.getLease() != null ? payment.getLease().getId() : null,
                payment.getInstallment() != null ? payment.getInstallment().getId() : null,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod() != null ? payment.getMethod().name() : null,
                payment.getStatus() != null ? payment.getStatus().name() : null,
                payment.getType() != null ? payment.getType().name() : null,
                payment.getPaidAt(),
                payment.getReceiptPdfUrl()
        );
    }

    public List<RentalInstallmentResponse> toResponseList(List<RentalInstallment> installments) {
        return installments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
