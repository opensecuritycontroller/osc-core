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
package org.osc.core.broker.service.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.osc.core.broker.service.dto.AlarmDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.util.log.LogProvider;
import org.osc.core.common.alarm.AlarmAction;
import org.osc.core.common.alarm.EventType;
import org.slf4j.Logger;

public class AlarmDtoValidator {

    private static final Logger log = LogProvider.getLogger(AlarmDtoValidator.class);

    public static void checkForNullFields(AlarmDto dto) throws Exception {
        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("name", dto.getName());
        map.put("eventType", dto.getEventType());
        map.put("severity", dto.getSeverity());
        map.put("alarmAction", dto.getAlarmAction());
        if (dto.getAlarmAction().equals(AlarmAction.EMAIL)) {
            map.put("email", dto.getReceipientEmail());
        }

        ValidateUtil.checkForNullFields(map);

    }

    public static void checkFieldLength(AlarmDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("name", dto.getName());
        map.put("regexMatch", dto.getRegexMatch());
        map.put("Email", dto.getReceipientEmail());

        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);

    }

    public static void checkRegexSyntax(AlarmDto dto) throws Exception {

        try {
            if (dto.getEventType().equals(EventType.JOB_FAILURE)) {
                Pattern.compile(dto.getRegexMatch());
            }
        } catch (PatternSyntaxException ex) {
            log.error("regexMatch syntax is invalid: " + ex.getMessage());
            throw new VmidcBrokerValidationException("regexMatch: " + dto.getRegexMatch() + " is invalid.");
        }
    }
}
