package com.openroof.openroof.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class MaxDigitsValidator implements ConstraintValidator<MaxDigits, Object> {

    private int maxDigits;

    @Override
    public void initialize(MaxDigits constraintAnnotation) {
        this.maxDigits = constraintAnnotation.integer();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BigDecimal bd;
        if (value instanceof BigDecimal) {
            bd = (BigDecimal) value;
        } else if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            bd = new BigDecimal(value.toString());
        } else if (value instanceof Double || value instanceof Float) {
            bd = BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            try {
                bd = new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (value instanceof Number) {
            bd = new BigDecimal(value.toString());
        } else {
            return false;
        }

        String plain = bd.abs().toPlainString();
        int dotIdx = plain.indexOf('.');
        String integerPart = dotIdx < 0 ? plain : plain.substring(0, dotIdx);

        return integerPart.length() <= maxDigits;
    }
}
