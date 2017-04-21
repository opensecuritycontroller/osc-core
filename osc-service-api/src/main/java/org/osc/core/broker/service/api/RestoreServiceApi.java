package org.osc.core.broker.service.api;

import org.osc.core.broker.service.request.RestoreRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public interface RestoreServiceApi
        extends ServiceDispatcherApi<RestoreRequest, EmptySuccessResponse> {
}
