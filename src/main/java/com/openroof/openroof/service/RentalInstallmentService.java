package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.RentalInstallmentMapper;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.repository.RentalInstallmentRepository;
import com.openroof.openroof.security.LeaseSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RentalInstallmentService {

    private final RentalInstallmentRepository installmentRepository;
    private final RentalInstallmentMapper installmentMapper;
    private final LeaseSecurity leaseSecurity;

    public Page<RentalInstallmentResponse> listByLease(Long leaseId, Long userId, Pageable pageable) {
        leaseSecurity.assertInstallmentAccess(userId, leaseId);
        return installmentRepository.findByLeaseIdOrderByDueDateDesc(leaseId, pageable)
                .map(installmentMapper::toResponse);
    }

    public RentalInstallmentResponse getById(Long installmentId, Long userId) {
        RentalInstallment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Installment", "id", installmentId));
        leaseSecurity.assertInstallmentAccess(userId, installment.getLease().getId());
        return installmentMapper.toResponse(installment);
    }
}
