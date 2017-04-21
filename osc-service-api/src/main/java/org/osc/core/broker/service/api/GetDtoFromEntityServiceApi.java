package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseDtoResponse;

public interface GetDtoFromEntityServiceApi<R extends BaseDto>
        extends ServiceDispatcherApi<GetDtoFromEntityRequest, BaseDtoResponse<R>> {
}
