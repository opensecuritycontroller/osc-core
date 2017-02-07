package org.osc.core.broker.validator.annotations;

import org.osc.core.broker.validator.RegexValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Documented
@Constraint(validatedBy = RegexValidator.class)
@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Regex {
    String message() default "regex is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}