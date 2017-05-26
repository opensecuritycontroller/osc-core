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
package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.events.DaiFailureType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.sdncontroller.AgentStatusElementImpl;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.api.NsxUpdateAgentsServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.Agent;
import org.osc.core.broker.service.request.NsxUpdateAgentsRequest;
import org.osc.core.broker.service.response.NsxUpdateAgentsResponse;
import org.osc.core.broker.service.response.NsxUpdateAgentsResponse.UpdatedAgent;
import org.osc.core.broker.service.response.NsxUpdateAgentsResponse.UpdatedAgents;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCreateMemberDeviceTask;
import org.osc.core.util.NetworkUtil;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;
import org.osc.sdk.sdn.api.AgentApi;
import org.osc.sdk.sdn.element.AgentElement;
import org.osc.sdk.sdn.element.AgentStatusElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = {NsxUpdateAgentsService.class, NsxUpdateAgentsServiceApi.class})
public class NsxUpdateAgentsService extends ServiceDispatcher<NsxUpdateAgentsRequest, NsxUpdateAgentsResponse>
        implements NsxUpdateAgentsServiceApi {

    private static final Logger LOG = Logger.getLogger(NsxUpdateAgentsService.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private AlertGenerator alertGenerator;

    @Reference
    private MgrCreateMemberDeviceTask mgrCreateMemberDeviceTask;

    @Override
    public NsxUpdateAgentsResponse exec(NsxUpdateAgentsRequest request, EntityManager em) throws Exception {

        validate(em, request);

        UpdatedAgents uas = new UpdatedAgents();
        List<UpdatedAgent> ual = new ArrayList<UpdatedAgent>();

        for (Agent agent : request.fabricAgents.list) {
            LOG.info("Agent: " + agent.toString());
            if (agent.allocatedIpAddress == null || agent.allocatedIpAddress.ipAddress == null) {
                continue;
            }

            UpdatedAgent ua = new UpdatedAgent();
            ua.agentId = agent.agentId;
            ua.responseString = new Date().toString();
            ual.add(ua);

            DistributedApplianceInstance dai = DistributedApplianceInstanceEntityMgr.findByNsxAgentIdAndNsxIp(em,
                    agent.agentId, request.nsxIpAddress);

            if (dai == null) {
                String host = NetworkUtil.resolveIpToName(request.nsxIpAddress);
                if (host != null) {
                    dai = DistributedApplianceInstanceEntityMgr.findByNsxAgentIdAndNsxIp(em, agent.agentId, host);
                }
            }

            if (dai != null) {
                updateNsxAgentInfo(em, dai, agent);
            } else {
                LOG.info("Unregistered deployed appliance detected (" + agent + "). Creating a new DAI.");
                createNewDAI(em, agent);
            }
        }

        uas.updatedAgent = ual;

        NsxUpdateAgentsResponse response = new NsxUpdateAgentsResponse();
        response.updatedAgents = uas;

        return response;
    }

    public void updateNsxAgentInfo(EntityManager em, DistributedApplianceInstance dai, String status) {
        updateNsxAgentInfo(em, dai, null, status);
    }

    public void updateNsxAgentInfo(EntityManager em, DistributedApplianceInstance dai, AgentElement agent) {
        updateNsxAgentInfo(em, dai, agent, null);
    }

    public void updateNsxAgentInfo(EntityManager em, DistributedApplianceInstance dai) {
        updateNsxAgentInfo(em, dai, null, null);
    }

    private void updateNsxAgentInfo(EntityManager em, DistributedApplianceInstance dai, AgentElement agent, String status) {
        // Locate and update DAI's NSX agent id, if not already set.
        if (dai.getNsxAgentId() == null) {

            if (agent == null) {
                agent = findNsxAgent(dai);
            }

            if (agent != null) {
                dai.setNsxAgentId(agent.getId());
                dai.setNsxHostId(agent.getHostId());
                dai.setNsxHostName(agent.getHostName());
                dai.setNsxVmId(agent.getVmId());
                dai.setNsxHostVsmUuid(agent.getHostVsmId());
                dai.setMgmtGateway(agent.getGateway());
                dai.setMgmtSubnetPrefixLength(agent.getSubnetPrefixLength());
                LOG.info("Associate DAI " + dai.getName() + " with NSX agent " + dai.getNsxAgentId());
                OSCEntityManager.update(em, dai, this.txBroadcastUtil);
            }
        }

        // Update NSX agent status
        if (dai.getNsxAgentId() != null) {
            try {
                AgentApi agentApi = this.apiFactoryService.createAgentApi(dai.getVirtualSystem());
                String currentStatus = null;
                AgentStatusElement agentStatus;

                // Optimization: try to get current status from nsx agent, if one was provided to us.
                // Otherwise, will make a call to NSX to get health status
                if (agent != null && agent.getStatus() != null) {
                    currentStatus = agent.getStatus();
                } else {
                    agentStatus = agentApi.getAgentStatus(dai.getNsxAgentId());
                    currentStatus = agentStatus.getStatus();
                }

                AgentStatusElementImpl newStatus;
                // Calculate what status is ought to be
                if (status == null) {
                    if (dai.getDiscovered() != null && dai.getInspectionReady() != null && dai.getDiscovered() && dai.getInspectionReady()) {
                        newStatus = new AgentStatusElementImpl("UP", null);
                    } else if (dai.getDiscovered() != null && dai.getDiscovered()) {
                        newStatus = new AgentStatusElementImpl("WARNING", "Appliance is not ready for inspection.");
                    } else {
                        newStatus = new AgentStatusElementImpl("DOWN", "Appliance is not discovered and/or ready (discovery:"
                                + dai.getDiscovered() + ", ready:" + dai.getInspectionReady() + ").");
                    }
                } else {
                    newStatus = new AgentStatusElementImpl(status, "Appliance status is " + status + ".");
                }

                // If it is status is different then what it should be - update it.
                if (currentStatus == null || !newStatus.getStatus().equals(currentStatus)) {
                    LOG.info("Update NSX agent " + dai.getNsxAgentId() + " status from " + currentStatus + " to " + newStatus.getStatus());
                    agentApi.updateAgentStatus(dai.getNsxAgentId(), newStatus);
                }
            } catch (Exception e) {
                LOG.error("Fail to set health status for NSX agent " + dai.getNsxAgentId(), e);
                this.alertGenerator.processDaiFailureEvent(DaiFailureType.DAI_NSX_STATUS_UPDATE,
                        new LockObjectReference(dai),
                        "Fail to set NSX Health Status for Appliance Instance '" + dai.getName() + "'");
            }
        }
    }

    private AgentElement findNsxAgent(DistributedApplianceInstance dai) {
        try {
            AgentApi agentApi = this.apiFactoryService.createAgentApi(dai.getVirtualSystem());
            List<AgentElement> agents = agentApi.getAgents(dai.getVirtualSystem().getNsxServiceId());
            for (AgentElement agent : agents) {
                if (agent.getIpAddress() == null) {
                    continue;
                }
                if (agent.getIpAddress().equals(dai.getIpAddress())) {
                    return agent;
                }
            }
        } catch (Exception ex) {
            LOG.error("Fail to retrieve Nsx Agents", ex);
        }
        return null;
    }

    private void validate(EntityManager em, NsxUpdateAgentsRequest request) throws Exception {
        if (request.fabricAgents == null) {
            throw new VmidcBrokerValidationException("Missing nsx agent list.");
        }

        if (request.nsxIpAddress == null) {
            throw new VmidcBrokerValidationException("Missing nsx IP address.");
        }

        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, em, this.txBroadcastUtil);
        VirtualizationConnector vc = emgr.findByFieldName("controllerIpAddress", request.nsxIpAddress);

        if (vc == null) {
            String host = NetworkUtil.resolveIpToName(request.nsxIpAddress);
            if (host != null) {
                vc = emgr.findByFieldName("controllerIpAddress", host);
            }
            if (vc == null) {
                throw new VmidcBrokerValidationException("NSX manager with IP address '" + host + "' not found.");
            }
        }
    }

    private void createNewDAI(EntityManager em, Agent agent) throws Exception {
        VirtualSystem vs = VirtualSystemEntityMgr.findByNsxServiceId(em,  agent.serviceId);

        // If we can't find the VS, that means this is a zombie VS call.
        if (vs == null) {
            LOG.warn(String.format("No virtual system found for the service id %s. A DAI will not be created.", agent.serviceId));
            return;
        }

        DistributedApplianceInstance dai = new DistributedApplianceInstance(vs);
        dai.setIpAddress(agent.getIpAddress());
        // Setting a temporary name since it is mandatory field.
        dai.setMgmtIpAddress(agent.getIpAddress());
        dai.setMgmtGateway(agent.getGateway());
        dai.setMgmtSubnetPrefixLength(agent.getSubnetPrefixLength());

        dai.setNsxAgentId(agent.getId());
        dai.setNsxHostId(agent.getHostId());
        dai.setNsxHostName(agent.getHostName());
        dai.setNsxVmId(agent.getVmId());
        dai.setNsxHostVsmUuid(agent.getHostVsmId());
        dai.setMgmtGateway(agent.getGateway());

        dai.setName("Temporary" + UUID.randomUUID().toString());

        dai = OSCEntityManager.create(em, dai, this.txBroadcastUtil);
        LOG.info("Created new DAI " + dai);

        // Generate a unique, intuitive and immutable name
        String applianceName = vs.getName() + "-" + dai.getId().toString();
        dai.setName(applianceName);

        OSCEntityManager.update(em, dai, this.txBroadcastUtil);

        checkMemberDevice(dai, em);
    }

    private void checkMemberDevice(DistributedApplianceInstance dai, EntityManager em) throws Exception {
        try (ManagerDeviceApi mgrApi = this.apiFactoryService.createManagerDeviceApi(dai.getVirtualSystem())) {
            if (mgrApi.isDeviceGroupSupported()) {
                this.mgrCreateMemberDeviceTask.createMemberDevice(em, dai, mgrApi);
            } else {
                ManagerDeviceMemberElement mgrDeviceMember = mgrApi.findDeviceMemberByName(dai.getName());
                if (mgrDeviceMember != null) {
                    updateDAIManagerId(em, dai, mgrDeviceMember);
                }
            }
        }
    }

    private void updateDAIManagerId(EntityManager em, DistributedApplianceInstance dai,
            ManagerDeviceMemberElement mgrDeviceMember) {
        if (dai != null && !mgrDeviceMember.getId().equals(dai.getMgrDeviceId())) {
            dai.setMgrDeviceId(mgrDeviceMember.getId());
            OSCEntityManager.update(em, dai, this.txBroadcastUtil);
        }
    }
}
