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
package org.osc.core.broker.service.tasks.conformance.openstack.sfc;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.sdn.NetworkElementImpl;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SfcFlowClassifierCreateTask.class)
public class SfcFlowClassifierCreateTask extends TransactionalTask {

    @Reference
    private ApiFactoryService apiFactory;

    private ServiceFunctionChain sfc;
    private SecurityGroup securityGroup;
    private VMPort port;
    private String vmName;

    public SfcFlowClassifierCreateTask create(SecurityGroup securityGroup, VMPort port) {
        SfcFlowClassifierCreateTask task = new SfcFlowClassifierCreateTask();

        task.securityGroup = securityGroup;
        task.sfc = securityGroup.getServiceFunctionChain();
        task.port = port;
        task.vmName = port.getVm() != null ? port.getVm().getName() : port.getSubnet().getName();
        task.apiFactory = this.apiFactory;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.securityGroup = em.find(SecurityGroup.class, this.securityGroup.getId());

        String sfcId = this.securityGroup.getNetworkElementId();
        DefaultInspectionPort inspectionPort = new DefaultInspectionPort();
        inspectionPort.setElementId(sfcId);

        try (SdnRedirectionApi redirApi = this.apiFactory
                .createNetworkRedirectionApi(this.securityGroup.getVirtualizationConnector())) {
            String fcId = redirApi.installInspectionHook(new NetworkElementImpl(this.port), inspectionPort, null, null,
                    null, null);
            this.port.setInspectionHookId(fcId);
            em.merge(this.port);
        }
    }

    @Override
    public String getName() {
        return String.format(
                "Creating flow classifier for port '%s' belonging to Security Group Member '%s' using Service Function Chain '%s'",
                this.port.getOpenstackId(), this.vmName, this.sfc.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sfc, this.securityGroup);
    }

}
