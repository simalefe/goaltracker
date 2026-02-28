package com.goaltracker.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class EndDateAfterStartDateValidator implements ConstraintValidator<EndDateAfterStartDate, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            java.lang.reflect.Method getStart = value.getClass().getMethod("getStartDate");
            java.lang.reflect.Method getEnd = value.getClass().getMethod("getEndDate");
            Object s = getStart.invoke(value);
            Object e = getEnd.invoke(value);
            if (s == null || e == null) return true; // other validators will catch nulls if required
            if (!(s instanceof LocalDate) || !(e instanceof LocalDate)) return true;
            LocalDate start = (LocalDate) s;
            LocalDate end = (LocalDate) e;
            return end.isAfter(start);
        } catch (NoSuchMethodException ex) {
            return true;
        } catch (Exception ex) {
            return true;
        }
    }
}

