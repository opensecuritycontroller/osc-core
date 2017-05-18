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
package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.api.RestConstants;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.Service;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.PasswordUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.VersionUtil;
import org.osc.sdk.sdn.api.ServiceApi;
import org.osc.sdk.sdn.element.ServiceElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CreateNsxServiceTask.class)
public class CreateNsxServiceTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(CreateNsxServiceTask.class);

    @Reference
    PasswordUtil passwordUtil;

    private VirtualSystem vs;

    public CreateNsxServiceTask create(VirtualSystem vs) {
        CreateNsxServiceTask task = new CreateNsxServiceTask();
        task.vs = vs;
        task.name = task.getName();
        task.passwordUtil = this.passwordUtil;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        LOG.debug("Start executing CreateNsxServiceTask for vs " + this.vs.getId());

        this.vs = em.find(VirtualSystem.class, this.vs.getId());
        ServiceApi serviceApi = VMwareSdnApiFactory.createServiceApi(this.vs);
        ServiceElement service = serviceApi.findService(this.vs.getDistributedAppliance().getName());
        String serviceId = service == null ? null : service.getId();
        if (serviceId == null) {
            String serviceFunctionalityType = ManagerApiFactory.getExternalServiceName(this.vs);

            service = new Service(
                    null,
                    null,
                    this.vs.getDistributedAppliance().getName(),
                    VersionUtil.getVersion().getShortVersionStr(),
                    this.vs.getNsxServiceManagerId().toString(),
                    serviceFunctionalityType,
                    this.vs.getId().toString(),
                    RestConstants.VMIDC_NSX_LOGIN,
                    this.passwordUtil.getVmidcNsxPass(),
                    ServerUtil.getServerIP(),
                    this.vs.getApplianceSoftwareVersion().getAppliance().getModel(),
                    this.vs.getApplianceSoftwareVersion().getApplianceSoftwareVersion());

            serviceId = serviceApi.createService(service);
        }

        this.vs.setNsxServiceId(serviceId);
        OSCEntityManager.update(em, this.vs, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Register Service '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
