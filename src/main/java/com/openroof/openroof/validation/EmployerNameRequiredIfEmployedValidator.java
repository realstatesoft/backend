package com.openroof.openroof.validation;

import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.model.enums.EmploymentStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmployerNameRequiredIfEmployedValidator
        implements ConstraintValidator<EmployerNameRequiredIfEmployed, CreateRentalApplicationRequest> {

    @Override
    public boolean isValid(CreateRentalApplicationRequest req, ConstraintValidatorContext ctx) {
        if (req == null) {
            return true;
        }
        if (req.employmentStatus() != EmploymentStatus.EMPLOYED) {
            return true;
        }
        boolean valid = req.employerName() != null && !req.employerName().isBlank();
        if (!valid) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(ctx.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("employerName")
                    .addConstraintViolation();
        }
        return valid;
    }
}
