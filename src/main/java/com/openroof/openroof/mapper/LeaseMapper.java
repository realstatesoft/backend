package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.LeaseSummaryResponse;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class LeaseMapper {

    public LeaseResponse toResponse(Lease lease) {
        return new LeaseResponse(
                lease.getId(),
                lease.getProperty() != null ? lease.getProperty().getId() : null,
                lease.getProperty() != null ? lease.getProperty().getTitle() : null,
                lease.getLandlord() != null ? lease.getLandlord().getId() : null,
                lease.getPrimaryTenant() != null ? lease.getPrimaryTenant().getId() : null,
                lease.getPrimaryTenant() != null ? lease.getPrimaryTenant().getName() : null,
                lease.getLeaseType(),
                lease.getStatus(),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getMonthlyRent(),
                lease.getSecurityDeposit(),
                lease.getDepositStatus(),
                lease.getBillingFrequency(),
                lease.isSigned() ? lease.getSignedByTenantAt() : null,
                lease.getActivatedAt(),
                lease.getCreatedAt()
        );
    }

    public LeaseSummaryResponse toSummaryResponse(Lease lease) {
        return new LeaseSummaryResponse(
                lease.getId(),
                lease.getProperty() != null ? lease.getProperty().getTitle() : null,
                lease.getPrimaryTenant() != null ? lease.getPrimaryTenant().getName() : null,
                lease.getStatus(),
                lease.getMonthlyRent(),
                lease.getStartDate(),
                lease.getEndDate()
        );
    }

    public Lease toEntity(CreateLeaseRequest dto, Property property, User tenant, User landlord) {
        Objects.requireNonNull(dto, "dto is required");
        Objects.requireNonNull(property, "property is required");
        Objects.requireNonNull(tenant, "tenant is required");
        Objects.requireNonNull(landlord, "landlord is required");

        return Lease.builder()
                .property(property)
                .primaryTenant(tenant)
                .landlord(landlord)
                .leaseType(dto.leaseType())
                .status(LeaseStatus.DRAFT)
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .monthlyRent(dto.monthlyRent())
                .securityDeposit(dto.securityDeposit())
                .billingFrequency(dto.billingFrequency())
                .lateFeeType(dto.lateFeeType())
                .lateFeeValue(dto.lateFeeAmount())
                .build();
    }

    public void updateEntity(Lease lease, CreateLeaseRequest dto) {
        lease.setLeaseType(dto.leaseType());
        lease.setStartDate(dto.startDate());
        lease.setEndDate(dto.endDate());
        lease.setMonthlyRent(dto.monthlyRent());
        lease.setSecurityDeposit(dto.securityDeposit());
        lease.setBillingFrequency(dto.billingFrequency());
        lease.setLateFeeType(dto.lateFeeType());
        lease.setLateFeeValue(dto.lateFeeAmount());
    }
}
