package org.osc.core.broker.service.api;

import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;

public interface SyncDeploymentSpecServiceApi extends ServiceDispatcherApi<BaseRequest<DeploymentSpecDto>, BaseJobResponse>
{
}
