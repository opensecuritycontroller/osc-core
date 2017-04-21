package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;

public interface UpdateDistributedApplianceServiceApi
        extends ServiceDispatcherApi<BaseRequest<DistributedApplianceDto>, BaseJobResponse> {
}
