package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.AddDistributedApplianceResponse;

public interface AddDistributedApplianceServiceApi
        extends ServiceDispatcherApi<BaseRequest<DistributedApplianceDto>, AddDistributedApplianceResponse> {
}
