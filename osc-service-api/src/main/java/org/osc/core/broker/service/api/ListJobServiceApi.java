package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.JobRecordDto;
import org.osc.core.broker.service.request.ListJobRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListJobServiceApi
        extends ServiceDispatcherApi<ListJobRequest, ListResponse<JobRecordDto>> {
}
