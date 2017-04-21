package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.TaskRecordDto;
import org.osc.core.broker.service.request.ListTaskRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListTaskServiceApi
        extends ServiceDispatcherApi<ListTaskRequest, ListResponse<TaskRecordDto>> {
}
