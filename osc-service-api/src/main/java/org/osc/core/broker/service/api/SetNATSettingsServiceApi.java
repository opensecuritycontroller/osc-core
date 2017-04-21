package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.NATSettingsDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.response.BaseJobResponse;

public interface SetNATSettingsServiceApi
        extends ServiceDispatcherApi<DryRunRequest<NATSettingsDto>, BaseJobResponse> {
}
