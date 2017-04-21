package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListDistributedApplianceInstanceServiceApi
        extends ServiceDispatcherApi<BaseRequest<BaseDto>, ListResponse<DistributedApplianceInstanceDto>> {
}
