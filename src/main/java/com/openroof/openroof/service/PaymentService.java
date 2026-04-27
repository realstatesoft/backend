package com.openroof.openroof.service;

import com.openroof.openroof.dto.payment.PaymentRequest;
import com.openroof.openroof.dto.payment.PaymentResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PaymentRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    // ADMIN: listar todos los pagos con filtros opcionales
    public Page<PaymentResponse> getAll(Long userId, PaymentStatus status, Pageable pageable) {
        if (userId != null && status != null) {
            return paymentRepository.findByUser_IdAndStatus(userId, status, pageable).map(this::toResponse);
        }
        if (userId != null) {
            return paymentRepository.findByUser_Id(userId, pageable).map(this::toResponse);
        }
        if (status != null) {
            return paymentRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return paymentRepository.findAll(pageable).map(this::toResponse);
    }

    // Usuario autenticado: ver sus propios pagos
    public Page<PaymentResponse> getMyPayments(String currentUserEmail, PaymentStatus status, Pageable pageable) {
        User user = getUserByEmail(currentUserEmail);
        if (status != null) {
            return paymentRepository.findByUser_IdAndStatus(user.getId(), status, pageable).map(this::toResponse);
        }
        return paymentRepository.findByUser_Id(user.getId(), pageable).map(this::toResponse);
    }

    // Ver un pago por ID (el usuario solo puede ver los suyos; ADMIN ve cualquiera)
    public PaymentResponse getById(Long id, String currentUserEmail) {
        Payment payment = getPaymentOrThrow(id);
        User user = getUserByEmail(currentUserEmail);
        if (user.getRole() != UserRole.ADMIN && !payment.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("No tienes permiso para ver este pago");
        }
        return toResponse(payment);
    }

    // Crear un pago nuevo en estado PENDING
    @Transactional
    public PaymentResponse create(PaymentRequest request, String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);

        Payment payment = Payment.builder()
                .user(user)
                .type(request.type())
                .status(PaymentStatus.PENDING)
                .concept(request.concept().trim())
                .amount(request.amount())
                .transactionCode(UUID.randomUUID().toString())
                .build();

        return toResponse(paymentRepository.save(payment));
    }

    // ADMIN: aprobar un pago
    @Transactional
    public PaymentResponse approvePayment(Long id) {
        Payment payment = getPaymentOrThrow(id);
        validateTransition(payment.getStatus(), PaymentStatus.APPROVED);
        payment.setStatus(PaymentStatus.APPROVED);
        return toResponse(paymentRepository.save(payment));
    }

    // ADMIN: rechazar un pago
    @Transactional
    public PaymentResponse rejectPayment(Long id) {
        Payment payment = getPaymentOrThrow(id);
        validateTransition(payment.getStatus(), PaymentStatus.REJECTED);
        payment.setStatus(PaymentStatus.REJECTED);
        return toResponse(paymentRepository.save(payment));
    }

    private void validateTransition(PaymentStatus current, PaymentStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == PaymentStatus.APPROVED || next == PaymentStatus.REJECTED;
            case APPROVED, REJECTED -> false;
        };
        if (!valid) {
            throw new BadRequestException("Transición de estado no permitida: " + current + " → " + next);
        }
    }

    private Payment getPaymentOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUser().getId(),
                payment.getUser().getName(),
                payment.getType(),
                payment.getStatus(),
                payment.getConcept(),
                payment.getTransactionCode(),
                payment.getAmount(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
