package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.request.ListApplianceSoftwareVersionRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListApplianceSoftwareVersionServiceApi
        extends ServiceDispatcherApi<ListApplianceSoftwareVersionRequest, ListResponse<ApplianceSoftwareVersionDto>> {
}
