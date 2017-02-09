package org.osc.core.broker.validator.annotations;

import org.osc.core.broker.validator.CustomAlarmDtoValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Documented
@Constraint(validatedBy = CustomAlarmDtoValidator.class)
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomAlarmDto {
    String message() default "If Alarm action is email, then alarm.recipientEmail is required";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}