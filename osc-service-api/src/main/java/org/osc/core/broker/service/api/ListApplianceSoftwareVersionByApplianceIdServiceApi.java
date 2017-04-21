package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.request.ListApplianceSoftwareVersionByApplianceIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListApplianceSoftwareVersionByApplianceIdServiceApi
        extends ServiceDispatcherApi<ListApplianceSoftwareVersionByApplianceIdRequest, ListResponse<ApplianceSoftwareVersionDto>> {
}
