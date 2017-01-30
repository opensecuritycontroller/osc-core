package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.service.dto.NATSettingsDto;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.util.ServerUtil;

public class GetNATSettingsService extends ServiceDispatcher<Request, BaseDtoResponse<NATSettingsDto>> {
    @Override
    public BaseDtoResponse<NATSettingsDto> exec(Request request, Session session) throws Exception {
        BaseDtoResponse<NATSettingsDto> response = new BaseDtoResponse<NATSettingsDto>();
        NATSettingsDto dto = new NATSettingsDto(ServerUtil.getServerIP());
        response.setDto(dto);
        return response;
    }
}
