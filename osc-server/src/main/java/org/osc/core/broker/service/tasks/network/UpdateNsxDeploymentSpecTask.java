/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.tasks.network;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.VersionedDeploymentSpec;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.RegisterDeploymentSpecTask;
import org.osc.sdk.sdn.api.DeploymentSpecApi;
import org.osc.sdk.sdn.element.DeploymentSpecElement;
import org.osgi.service.component.annotations.Component;

@Component(service=UpdateNsxDeploymentSpecTask.class)
public class UpdateNsxDeploymentSpecTask extends TransactionalTask {

    final Logger log = Logger.getLogger(UpdateNsxDeploymentSpecTask.class);

    private VirtualSystem vs;
    private VersionedDeploymentSpec deploySpec;

    public UpdateNsxDeploymentSpecTask create(VirtualSystem vs) {
        UpdateNsxDeploymentSpecTask task = new UpdateNsxDeploymentSpecTask();
        task.vs = vs;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    public UpdateNsxDeploymentSpecTask create(VirtualSystem vs, VersionedDeploymentSpec deploySpec) {
        UpdateNsxDeploymentSpecTask task = create(vs);
        task.deploySpec = deploySpec;
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        DeploymentSpecApi deploymentSpecApi = VMwareSdnApiFactory.createDeploymentSpecApi(this.vs);
        String imageName = this.vs.getApplianceSoftwareVersion().getImageUrl();
        if (this.deploySpec != null){
            this.deploySpec.setOvfUrl(RegisterDeploymentSpecTask.generateOvfUrl(imageName));
            deploymentSpecApi.updateDeploymentSpec(this.vs.getNsxServiceId(), this.deploySpec);
        } else {
            List<DeploymentSpecElement> deploymentSpecs = deploymentSpecApi.getDeploymentSpecs(this.vs.getNsxServiceId());
            for (DeploymentSpecElement deploymentSpec: CollectionUtils.emptyIfNull(deploymentSpecs)){
                VersionedDeploymentSpec ds = new VersionedDeploymentSpec(deploymentSpec);
                ds.setOvfUrl(RegisterDeploymentSpecTask.generateOvfUrl(imageName));
                deploymentSpecApi.updateDeploymentSpec(this.vs.getNsxServiceId(), ds);
            }
        }
    }

    @Override
    public String getName() {
        if (this.deploySpec != null) {
            return "Updating Deployment Specification of NSX Manager '" + this.vs.getVirtualizationConnector().getName()
                    + " for host version " + this.deploySpec.getHostVersion().toString() + "'";
        } else {
            return "Updating All Deployment Specifications of NSX Manager '" + this.vs.getVirtualizationConnector().getName()
                    + "'";
        }
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
