package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.dto.rental.RentalApplicationResponse;
import com.openroof.openroof.dto.rental.RentalApplicationSummaryResponse;
import com.openroof.openroof.model.enums.RentalApplicationStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.user.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RentalApplicationMapper {

    public RentalApplicationResponse toResponse(RentalApplication application) {
        return new RentalApplicationResponse(
                application.getId(),
                application.getProperty() != null ? application.getProperty().getId() : null,
                application.getProperty() != null ? application.getProperty().getTitle() : null,
                application.getApplicant() != null ? application.getApplicant().getId() : null,
                application.getApplicant() != null ? application.getApplicant().getName() : null,
                application.getStatus(),
                application.getMessage(),
                application.getMonthlyIncome(),
                application.getEmploymentStatus(),
                application.getNumberOfOccupants(),
                application.getHasPets(),
                application.getSubmittedAt(),
                application.getDecidedAt(),
                application.getCreatedAt()
        );
    }

    public RentalApplicationSummaryResponse toSummaryResponse(RentalApplication application) {
        return new RentalApplicationSummaryResponse(
                application.getId(),
                application.getProperty() != null ? application.getProperty().getTitle() : null,
                application.getApplicant() != null ? application.getApplicant().getName() : null,
                application.getStatus(),
                application.getSubmittedAt()
        );
    }

    public RentalApplication toEntity(CreateRentalApplicationRequest dto, Property property, User applicant) {
        return RentalApplication.builder()
                .property(property)
                .applicant(applicant)
                .status(RentalApplicationStatus.SUBMITTED)
                .monthlyIncome(dto.monthlyIncome())
                .employmentStatus(dto.employmentStatus())
                .employerName(dto.employerName())
                .message(dto.message())
                .numberOfOccupants(dto.numberOfOccupants())
                .hasPets(dto.hasPets())
                .screeningConsent(dto.acceptsTerms())
                .screeningConsentAt(Boolean.TRUE.equals(dto.acceptsTerms()) ? LocalDateTime.now() : null)
                .submittedAt(LocalDateTime.now())
                .build();
    }
}
