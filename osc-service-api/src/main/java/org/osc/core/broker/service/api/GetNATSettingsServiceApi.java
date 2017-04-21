package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.NATSettingsDto;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;

public interface GetNATSettingsServiceApi
        extends ServiceDispatcherApi<Request, BaseDtoResponse<NATSettingsDto>> {
}
