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

import org.osc.core.broker.model.entities.events.AlarmAction;
import org.osc.core.broker.service.alarm.AlarmDto;
import org.osc.core.broker.validator.annotations.CustomAlarmDto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * If Alarm action is email, then email is required
 * */
public class CustomAlarmDtoValidator implements ConstraintValidator<CustomAlarmDto, AlarmDto> {

    public void initialize(CustomAlarmDto alarm) {
        //
    }

    public boolean isValid(AlarmDto value, ConstraintValidatorContext context) {
        if(value!=null && AlarmAction.EMAIL.equals(value.getAlarmAction())){
            if (value.getReceipientEmail()!=null) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }
}