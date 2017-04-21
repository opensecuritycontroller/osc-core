package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.request.ListVirtualSystemRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListVirtualSystemServiceApi
        extends ServiceDispatcherApi<ListVirtualSystemRequest, ListResponse<VirtualSystemDto>> {
}
