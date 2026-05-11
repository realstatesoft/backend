package com.openroof.openroof.dto.rental;

import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.LateFeeType;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.enums.UtilityType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateLeaseRequest(

        @NotNull Long propertyId,
        @NotNull Long tenantId,
        @NotNull LeaseType leaseType,
        @NotNull @FutureOrPresent LocalDate startDate,
        LocalDate endDate,
        @NotNull @DecimalMin("0.0") BigDecimal monthlyRent,
        @NotNull @DecimalMin("0.0") BigDecimal securityDeposit,
        @NotNull BillingFrequency billingFrequency,
        LateFeeType lateFeeType,
        BigDecimal lateFeeAmount,
        List<UtilityType> includedUtilities

) {}
