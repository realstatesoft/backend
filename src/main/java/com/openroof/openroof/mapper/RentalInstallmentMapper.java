package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.model.rental.RentalInstallment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RentalInstallmentMapper {

    public RentalInstallmentResponse toResponse(RentalInstallment installment) {
        BigDecimal baseRent = installment.getBaseRent() != null ? installment.getBaseRent() : BigDecimal.ZERO;
        BigDecimal lateFee  = installment.getLateFee()  != null ? installment.getLateFee()  : BigDecimal.ZERO;

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
                installment.getCreatedAt()
        );
    }

    public List<RentalInstallmentResponse> toResponseList(List<RentalInstallment> installments) {
        return installments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
