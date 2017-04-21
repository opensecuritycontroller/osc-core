package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.service.request.ListUserRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListUserServiceApi
        extends ServiceDispatcherApi<ListUserRequest, ListResponse<UserDto>> {
}
