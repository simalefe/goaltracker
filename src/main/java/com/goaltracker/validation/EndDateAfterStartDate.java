package com.goaltracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = EndDateAfterStartDateValidator.class)
@Target({ TYPE })
@Retention(RUNTIME)
public @interface EndDateAfterStartDate {
    String message() default "Bitiş tarihi başlangıç tarihinden sonra olmalıdır.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

