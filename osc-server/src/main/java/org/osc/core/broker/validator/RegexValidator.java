/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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