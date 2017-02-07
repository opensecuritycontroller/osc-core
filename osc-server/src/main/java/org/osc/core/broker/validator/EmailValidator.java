package org.osc.core.broker.validator;

import org.osc.core.broker.validator.annotations.Email;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Check if annotated String is valid Email Pattern
 * */
public class EmailValidator implements ConstraintValidator<Email, String> {

    public void initialize(Email email) {
        //
    }

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value != null) {
            Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(value);
            return matcher.find();
        }
        return true;
    }
}