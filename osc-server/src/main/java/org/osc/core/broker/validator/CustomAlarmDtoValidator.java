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