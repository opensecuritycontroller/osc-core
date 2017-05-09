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

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.api.RestConstants;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.PasswordUtil;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.sdn.api.ServiceInstanceApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = UpdateNsxServiceInstanceAttributesTask.class)
public class UpdateNsxServiceInstanceAttributesTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(UpdateNsxServiceInstanceAttributesTask.class);

    @Reference
    public PasswordUtil passwordUtil;

    private VirtualSystem vs;

    public UpdateNsxServiceInstanceAttributesTask create(VirtualSystem vs) {
        UpdateNsxServiceInstanceAttributesTask task = new UpdateNsxServiceInstanceAttributesTask();
        task.vs = vs;
        task.name = task.getName();
        task.passwordUtil = this.passwordUtil;
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        log.info("Start executing UpdateNsxServiceInstanceAttributesTask");

        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        ServiceInstanceApi serviceInstanceApi = VMwareSdnApiFactory.createServiceInstanceApi(this.vs);

        serviceInstanceApi.updateServiceInstance(
                this.vs.getNsxServiceInstanceId(),
                this.vs.getNsxServiceId(),
                RestConstants.VMIDC_NSX_LOGIN,
                this.passwordUtil.getVmidcNsxPass(),
                ServerUtil.getServerIP(),
                this.vs.getDistributedAppliance().getApplianceVersion(),
                this.vs.getDistributedAppliance().getAppliance().getModel());
    }

    @Override
    public String getName() {
        return "Updating Service Instance Attributes of NSX Manager'" + this.vs.getVirtualizationConnector().getName()
                + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
