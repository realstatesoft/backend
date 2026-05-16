package com.openroof.openroof.service;

import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.enums.LeasePaymentStatus;
import com.openroof.openroof.model.enums.LeasePaymentType;
import com.openroof.openroof.model.enums.PaymentMethod;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.LeasePayment;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeasePaymentRepository;
import com.openroof.openroof.repository.RentalInstallmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalPaymentService {

    private final RentalInstallmentRepository installmentRepository;
    private final LeasePaymentRepository leasePaymentRepository;

    @Transactional
    public LeasePayment registerPayment(Long installmentId, User payer, BigDecimal amount, String method, String idempotencyKey, String notes) {
        RentalInstallment installment = installmentRepository.findByIdForUpdate(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada"));

        if (installment.getStatus() == InstallmentStatus.PAID) {
            throw new BadRequestException("La cuota ya está pagada");
        }

        if (installment.getStatus() == InstallmentStatus.WAIVED) {
            throw new BadRequestException("La cuota fue condonada");
        }

        BigDecimal balance = installment.getBalance();
        if (amount.compareTo(balance) > 0) {
            throw new BadRequestException("El monto excede el saldo pendiente de la cuota");
        }

        PaymentMethod paymentMethod = resolveMethod(method);

        Lease lease = installment.getLease();

        java.util.Optional<LeasePayment> existingPayment = leasePaymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        LeasePayment payment = LeasePayment.builder()
                .lease(lease)
                .installment(installment)
                .payer(payer)
                .amount(amount)
                .currency("PYG")
                .method(paymentMethod)
                .status(LeasePaymentStatus.COMPLETED)
                .type(LeasePaymentType.RENT)
                .idempotencyKey(idempotencyKey)
                .notes(notes)
                .paidAt(LocalDateTime.now())
                .build();

        leasePaymentRepository.save(payment);

        BigDecimal currentPaid = installment.getPaidAmount() == null ? BigDecimal.ZERO : installment.getPaidAmount();
        BigDecimal newPaid = currentPaid.add(amount);
        installment.setPaidAmount(newPaid);

        if (newPaid.compareTo(installment.getTotalAmount()) >= 0) {
            installment.setStatus(InstallmentStatus.PAID);
            installment.setPaidDate(LocalDate.now());
        } else {
            installment.setStatus(InstallmentStatus.PARTIAL);
        }

        installmentRepository.save(installment);

        return payment;
    }

    private PaymentMethod resolveMethod(String method) {
        if (method == null || method.isBlank()) {
            return PaymentMethod.OTHER;
        }
        return switch (method.toUpperCase()) {
            case "TRANSFER" -> PaymentMethod.BANK_TRANSFER;
            case "CASH" -> PaymentMethod.CASH;
            case "CHECK" -> PaymentMethod.CHECK;
            case "ACH" -> PaymentMethod.ACH;
            case "CARD" -> PaymentMethod.CARD;
            case "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER;
            default -> PaymentMethod.OTHER;
        };
    }
}
