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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.Service;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.VersionUtil;
import org.osc.sdk.sdn.api.ServiceApi;
import org.osc.sdk.sdn.element.ServiceElement;

public class CreateNsxServiceTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(CreateNsxServiceTask.class);

    private VirtualSystem vs;

    public CreateNsxServiceTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        LOG.debug("Start executing CreateNsxServiceTask for vs " + this.vs.getId());

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        ServiceApi serviceApi = VMwareSdnApiFactory.createServiceApi(this.vs);
        ServiceElement service = serviceApi.findService(this.vs.getDistributedAppliance().getName());
        String serviceId = service == null ? null : service.getId();
        if (serviceId == null) {
            String serviceFunctionalityType = ManagerApiFactory
                    .createApplianceManagerApi(
                            this.vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                    .getNsxServiceName();

            service = new Service(
                    null,
                    null,
                    this.vs.getDistributedAppliance().getName(),
                    VersionUtil.getVersion().getShortVersionStr(),
                    this.vs.getNsxServiceManagerId().toString(),
                    serviceFunctionalityType,
                    this.vs.getId().toString(),
                    AgentAuthFilter.VMIDC_AGENT_LOGIN,
                    EncryptionUtil.encryptAESCTR(AgentAuthFilter.VMIDC_AGENT_PASS),
                    ServerUtil.getServerIP(),
                    this.vs.getApplianceSoftwareVersion().getAppliance().getModel(),
                    this.vs.getApplianceSoftwareVersion().getApplianceSoftwareVersion());

            serviceId = serviceApi.createService(service);
        }

        this.vs.setNsxServiceId(serviceId);
        EntityManager.update(session, this.vs);
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
