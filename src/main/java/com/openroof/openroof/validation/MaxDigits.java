package com.openroof.openroof.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxDigitsValidator.class)
public @interface MaxDigits {
    int integer() default 20;
    String message() default "El campo no puede tener más de {integer} dígitos enteros";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
