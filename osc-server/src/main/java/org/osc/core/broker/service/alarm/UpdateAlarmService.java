package org.osc.core.broker.service.alarm;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.AlarmEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.util.ValidateUtil;

public class UpdateAlarmService extends ServiceDispatcher<BaseRequest<AlarmDto>, BaseResponse> {

    @Override
    public BaseResponse exec(BaseRequest<AlarmDto> request, Session session) throws Exception {

        EntityManager<Alarm> emgr = new EntityManager<Alarm>(Alarm.class, session);

        // retrieve existing entry from db
        Alarm alarm = emgr.findByPrimaryKey(request.getDto().getId());

        // this validate function will throw exception if entry is not unique,
        // has empty fields, violates correct formatting or exceeds maximum allowed length
        validate(session, request.getDto(), alarm, emgr);

        AlarmEntityMgr.toEntity(alarm, request.getDto());
        emgr.update(alarm);
        BaseResponse response = new BaseResponse(alarm.getId());

        return response;
    }

    void validate(Session session, AlarmDto dto, Alarm existingAlarm, EntityManager<Alarm> emgr) throws Exception {

        BaseDto.checkForNullId(dto);

        // check for null/empty values
        AlarmDto.checkForNullFields(dto);
        //field length should not exceed current MAX_LEN = 155 chars
        AlarmDto.checkFieldLength(dto);

        //validating email address formatting
        if (!StringUtils.isBlank(dto.getReceipientEmail())) {
            ValidateUtil.checkForValidEmailAddress(dto.getReceipientEmail());
        }

        //validating the user entered regex syntax
        AlarmDto.checkRegexSyntax(dto);

        // entry must pre-exist in db
        if (existingAlarm == null) {

            throw new VmidcBrokerValidationException("Alarm entry with name " + dto.getName() + " is not found.");
        }
    }
}
