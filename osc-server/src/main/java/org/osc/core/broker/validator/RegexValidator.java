package org.osc.core.broker.validator;

import org.osc.core.broker.validator.annotations.Regex;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Check if annotated String is valid Regex Pattern
 * */
public class RegexValidator implements ConstraintValidator<Regex, String> {

    public void initialize(Regex reges) {
        //
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        try {
            if(value!=null) {
                Pattern.compile(value);
            }
        } catch (PatternSyntaxException exception) {
            return false;
        }
        return true;
    }
}