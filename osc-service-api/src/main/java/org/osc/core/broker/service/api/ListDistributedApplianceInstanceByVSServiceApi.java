package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListDistributedApplianceInstanceByVSServiceApi
        extends ServiceDispatcherApi<BaseIdRequest, ListResponse<DistributedApplianceInstanceDto>> {
}
