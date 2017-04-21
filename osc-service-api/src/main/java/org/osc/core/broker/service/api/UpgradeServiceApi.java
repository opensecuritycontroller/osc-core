package org.osc.core.broker.service.api;

import org.osc.core.broker.service.request.UpgradeRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public interface UpgradeServiceApi
        extends ServiceDispatcherApi<UpgradeRequest, EmptySuccessResponse> {
}
