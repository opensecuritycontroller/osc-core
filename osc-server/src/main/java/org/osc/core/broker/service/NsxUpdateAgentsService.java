package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.AgentStatus;
import org.osc.core.broker.model.entities.appliance.AgentType;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.events.DaiFailureType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.Agent;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.rest.server.api.proprietary.NsxApis.UpdatedAgent;
import org.osc.core.broker.rest.server.api.proprietary.NsxApis.UpdatedAgents;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.NsxUpdateAgentsRequest;
import org.osc.core.broker.service.response.NsxUpdateAgentsResponse;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCreateMemberDeviceTask;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.NetworkUtil;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;
import org.osc.sdk.sdn.api.AgentApi;
import org.osc.sdk.sdn.element.AgentElement;
import org.osc.sdk.sdn.element.AgentStatusElement;

public class NsxUpdateAgentsService extends ServiceDispatcher<NsxUpdateAgentsRequest, NsxUpdateAgentsResponse> {

    private static final Logger LOG = Logger.getLogger(NsxUpdateAgentsService.class);

    @Override
    public NsxUpdateAgentsResponse exec(NsxUpdateAgentsRequest request, Session session) throws Exception {

        validate(session, request);

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

            DistributedApplianceInstance dai = DistributedApplianceInstanceEntityMgr.findByNsxAgentIdAndNsxIp(session,
                    agent.agentId, request.nsxIpAddress);

            if (dai != null) {
                updateNsxAgentInfo(session, dai, agent);
            } else {
                String host = NetworkUtil.resolveIpToName(request.nsxIpAddress);
                if (host != null) {
                    dai = DistributedApplianceInstanceEntityMgr.findByNsxAgentIdAndNsxIp(session, agent.agentId, host);
                }
                if (dai != null) {
                    updateNsxAgentInfo(session, dai, agent);
                } else {
                    LOG.info("Unregistered deployed appliance detected (" + agent + "). Creating a new DAI.");
                    createNewDAI(session, agent);
                }
            }
        }

        uas.updatedAgent = ual;

        NsxUpdateAgentsResponse response = new NsxUpdateAgentsResponse();
        response.updatedAgents = uas;

        return response;
    }

    public static void updateNsxAgentInfo(Session session, DistributedApplianceInstance dai, String status) {
        updateNsxAgentInfo(session, dai, null, status);
    }

    public static void updateNsxAgentInfo(Session session, DistributedApplianceInstance dai, AgentElement agent) {
        updateNsxAgentInfo(session, dai, agent, null);
    }

    public static void updateNsxAgentInfo(Session session, DistributedApplianceInstance dai) {
        updateNsxAgentInfo(session, dai, null, null);
    }

    private static void updateNsxAgentInfo(Session session, DistributedApplianceInstance dai, AgentElement agent, String status) {
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
                EntityManager.update(session, dai);
            }
        }

        // Update NSX agent status
        if (dai.getNsxAgentId() != null) {
            try {
                AgentApi agentApi = VMwareSdnApiFactory.createAgentApi(dai.getVirtualSystem());
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

                AgentStatus newStatus;
                // Calculate what status is ought to be
                if (status == null) {
                    if (dai.getDiscovered() != null && dai.getInspectionReady() != null && dai.getDiscovered() && dai.getInspectionReady()) {
                        newStatus = new AgentStatus("UP", null);
                    } else if (dai.getDiscovered() != null && dai.getDiscovered()) {
                        newStatus = new AgentStatus("WARNING", "Appliance is not ready for inspection.");
                    } else {
                        newStatus = new AgentStatus("DOWN", "Appliance is not discovered and/or ready (discovery:"
                                + dai.getDiscovered() + ", ready:" + dai.getInspectionReady() + ").");
                    }
                } else {
                    newStatus = new AgentStatus(status, "Appliance status is " + status + ".");
                }

                // If it is status is different then what it should be - update it.
                if (currentStatus == null || !newStatus.getStatus().equals(currentStatus)) {
                    LOG.info("Update NSX agent " + dai.getNsxAgentId() + " status from " + currentStatus + " to " + newStatus.getStatus());
                    agentApi.updateAgentStatus(dai.getNsxAgentId(), newStatus);
                }
            } catch (Exception e) {
                LOG.error("Fail to set health status for NSX agent " + dai.getNsxAgentId(), e);
                AlertGenerator.processDaiFailureEvent(DaiFailureType.DAI_NSX_STATUS_UPDATE,
                        new LockObjectReference(dai),
                        "Fail to set NSX Health Status for Appliance Instance '" + dai.getName() + "'");
            }
        }
    }

    private static AgentElement findNsxAgent(DistributedApplianceInstance dai) {
        try {
            AgentApi agentApi = VMwareSdnApiFactory.createAgentApi(dai.getVirtualSystem());
            for (AgentElement agent : agentApi.getAgents(dai.getVirtualSystem().getNsxServiceId())) {
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

    private void validate(Session session, NsxUpdateAgentsRequest request) throws Exception {
        if (request.fabricAgents == null) {
            throw new VmidcBrokerValidationException("Missing nsx agent list.");
        }

        if (request.nsxIpAddress == null) {
            throw new VmidcBrokerValidationException("Missing nsx IP address.");
        }

        EntityManager<VirtualizationConnector> emgr = new EntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, session);
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

    private void createNewDAI(Session session, Agent agent) throws Exception {
        VirtualSystem vs = VirtualSystemEntityMgr.findByNsxServiceId(session,  agent.serviceId);

        // If we cant find the VS, that means this is a zombie VS call.
        if (vs == null) {
            LOG.warn(String.format("No virtual system found for the service id %s. A DAI will not be created.", agent.serviceId));
        }

        // TODO emanoel: remove agent type.
        DistributedApplianceInstance dai = new DistributedApplianceInstance(vs, AgentType.AGENT);
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

        dai = EntityManager.create(session, dai);
        LOG.info("Created new DAI " + dai);

        // Generate a unique, intuitive and immutable name
        String applianceName = vs.getName() + "-" + dai.getId().toString();
        dai.setName(applianceName);
        dai.setPolicyMapOutOfSync(true);
        dai.setPassword(EncryptionUtil.encrypt(AgentAuthFilter.VMIDC_AGENT_PASS));

        EntityManager.update(session, dai);

        checkMemberDevice(dai, session);
    }

    private static void checkMemberDevice(DistributedApplianceInstance dai, Session session) throws Exception {
        try (ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(dai.getVirtualSystem())) {
            if (mgrApi.isDeviceGroupSupported()) {
                MgrCreateMemberDeviceTask.createMemberDevice(session, dai, mgrApi);
            } else {
                ManagerDeviceMemberElement mgrDeviceMember = mgrApi.findDeviceMemberByName(dai.getName());
                if (mgrDeviceMember != null) {
                    updateDAIManagerId(session, dai, mgrDeviceMember);
                }
            }
        }
    }

    private static void updateDAIManagerId(Session session, DistributedApplianceInstance dai,
            ManagerDeviceMemberElement mgrDeviceMember) {
        if (dai != null && !mgrDeviceMember.getId().equals(dai.getMgrDeviceId())) {
            dai.setMgrDeviceId(mgrDeviceMember.getId());
            EntityManager.update(session, dai);
        }
    }
}
