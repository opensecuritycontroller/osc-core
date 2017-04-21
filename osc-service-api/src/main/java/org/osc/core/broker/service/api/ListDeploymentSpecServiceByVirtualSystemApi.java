package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public interface ListDeploymentSpecServiceByVirtualSystemApi
        extends ServiceDispatcherApi<BaseIdRequest, ListResponse<DeploymentSpecDto>> {
}
