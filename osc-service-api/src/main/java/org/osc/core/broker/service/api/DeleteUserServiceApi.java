package org.osc.core.broker.service.api;

import org.osc.core.broker.service.request.DeleteUserRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public interface DeleteUserServiceApi
        extends ServiceDispatcherApi<DeleteUserRequest, EmptySuccessResponse> {
}
