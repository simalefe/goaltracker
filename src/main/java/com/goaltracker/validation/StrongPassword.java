package com.goaltracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Şifre en az 8 karakter, 1 büyük harf, 1 küçük harf ve 1 rakam içermelidir";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

