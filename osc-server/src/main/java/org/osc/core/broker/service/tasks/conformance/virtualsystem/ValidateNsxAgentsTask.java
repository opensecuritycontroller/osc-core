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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.NsxUpdateAgentsService;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrDeleteMemberDeviceTask;
import org.osc.sdk.sdn.api.AgentApi;
import org.osc.sdk.sdn.element.AgentElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ValidateNsxAgentsTask.class)
public class ValidateNsxAgentsTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(ValidateNsxAgentsTask.class);

    @Reference
    private MgrDeleteMemberDeviceTask mgrDeleteMemberDeviceTask;

    @Reference
    private NsxUpdateAgentsService nsxUpdateAgentsService;

    private VirtualSystem vs;

    public ValidateNsxAgentsTask create(VirtualSystem vs) {
        ValidateNsxAgentsTask task = new ValidateNsxAgentsTask();
        task.mgrDeleteMemberDeviceTask = this.mgrDeleteMemberDeviceTask;
        task.vs = vs;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        if (this.vs.getNsxServiceInstanceId() == null) {
            return;
        }

        AgentApi agentApi = VMwareSdnApiFactory.createAgentApi(this.vs);
        List<AgentElement> nsxAgents = agentApi.getAgents(this.vs.getNsxServiceId());

        // Check DAIs
        List<DistributedApplianceInstance> daiToBeDeleted = new ArrayList<DistributedApplianceInstance>();
        for (DistributedApplianceInstance dai : this.vs.getDistributedApplianceInstances()) {
            AgentElement agent = findAgent(nsxAgents, dai);
            if (agent == null) {
                // If no agent found and it once existed, we assume it got undeployed.
                if (dai.getNsxAgentId() != null) {
                    LOG.info("DAI '" + dai.getName()
                    + "' was not found in deployed agent list and thus should be removed.");
                    if (this.mgrDeleteMemberDeviceTask.deleteMemberDevice(dai)) {
                        daiToBeDeleted.add(dai);
                    }
                }
            } else {
                // Update DB with NSX agent info
                if (dai.getNsxAgentId() == null) {
                    dai.setNsxAgentId(agent.getId());
                    dai.setNsxHostId(agent.getHostId());
                    dai.setNsxHostName(agent.getHostName());
                    dai.setNsxVmId(agent.getVmId());
                    dai.setNsxHostVsmUuid(agent.getHostVsmId());
                    dai.setMgmtGateway(agent.getGateway());
                    dai.setMgmtSubnetPrefixLength(agent.getSubnetPrefixLength());
                    OSCEntityManager.update(em, dai, this.txBroadcastUtil);
                }
                this.nsxUpdateAgentsService.updateNsxAgentInfo(em, dai, agent);
            }
        }

        // Delete any DAIs deemed invalid
        for (DistributedApplianceInstance dai : daiToBeDeleted) {
            this.vs.removeDistributedApplianceInstance(dai);
            OSCEntityManager.delete(em, dai, this.txBroadcastUtil);
        }
    }

    private AgentElement findAgent(List<AgentElement> nsxAgents, DistributedApplianceInstance dai) {
        for (AgentElement agent : nsxAgents) {
            // Try to match Nsx Agent id first. Else try to fallback on Nsx
            // Agent IP
            if (dai.getNsxAgentId() != null && agent.getId().equals(dai.getNsxAgentId())) {
                return agent;
            } else if (agent.getIpAddress() != null && agent.getIpAddress().equals(dai.getIpAddress())) {
                return agent;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Validating NSX Agents for '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
