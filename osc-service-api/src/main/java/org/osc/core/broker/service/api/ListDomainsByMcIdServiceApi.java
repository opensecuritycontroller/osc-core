package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.DomainDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListDomainsByMcIdServiceApi
        extends ServiceDispatcherApi<BaseIdRequest, ListResponse<DomainDto>> {
}
