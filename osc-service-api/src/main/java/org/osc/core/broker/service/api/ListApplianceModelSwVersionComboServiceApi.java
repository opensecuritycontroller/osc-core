package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.ApplianceModelSoftwareVersionDto;
import org.osc.core.broker.service.request.ListApplianceModelSwVersionComboRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListApplianceModelSwVersionComboServiceApi
        extends ServiceDispatcherApi<ListApplianceModelSwVersionComboRequest, ListResponse<ApplianceModelSoftwareVersionDto>> {
}
