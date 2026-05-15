package com.openroof.openroof.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmployerNameRequiredIfEmployedValidator.class)
public @interface EmployerNameRequiredIfEmployed {

    String message() default "employerName es obligatorio cuando employmentStatus = EMPLOYED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
