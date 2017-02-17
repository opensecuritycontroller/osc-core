package org.osc.core.broker.service;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.AgentStatus;
import org.osc.core.broker.model.entities.appliance.AgentType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.events.DaiFailureType;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.AgentRegisterServiceRequest;
import org.osc.core.broker.service.request.AgentRegisterServiceRequestValidator;
import org.osc.core.broker.service.request.RequestValidator;
import org.osc.core.broker.service.response.AgentRegisterServiceResponse;
import org.osc.core.broker.service.tasks.agent.AgentInterfaceEndpointMapSetTask;
import org.osc.core.broker.service.tasks.agent.UpdateApplianceConsolePasswordTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCreateMemberDeviceTask;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.NetworkUtil;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;
import org.osc.sdk.sdn.api.AgentApi;
import org.osc.sdk.sdn.element.AgentElement;
import org.osc.sdk.sdn.element.AgentStatusElement;

public class AgentRegisterService extends ServiceDispatcher<AgentRegisterServiceRequest, AgentRegisterServiceResponse> {
    private RequestValidator<AgentRegisterServiceRequest, DistributedApplianceInstance> validator;

    private static final Logger LOG = Logger.getLogger(AgentRegisterService.class);

    @Override
    public AgentRegisterServiceResponse exec(AgentRegisterServiceRequest request, Session session) throws Exception {
        if (this.validator == null) {
            this.validator = new AgentRegisterServiceRequestValidator(session);
        }

        DistributedApplianceInstance dai = this.validator.validateAndLoad(request);

        /*
         * If IP had been re-purposed for a different VS:
         * It is possible that old appliance were un-deployed (handing IP address back to IP Pool,
         * and then re-deployed (reusing the same IP) before we were able to remove this staled entry,
         * Resulting in returning a NULL DAI which will result in creation of new DAI which now belongs
         * to a different VS/DA.
         */
        if (dai != null && !request.getVsId().equals(dai.getVirtualSystem().getId())) {
            // This is obviously a staled entry. Take the opportunity to remove it.
            LOG.info("A matching DAI had been located by IP '" + request.getApplianceIp()
            + "' but VS id don't match. Removing staled entry.");
            EntityManager.delete(session, dai);
            dai = null;
        }

        if (dai != null) {
            session.refresh(dai, new LockOptions(LockMode.PESSIMISTIC_WRITE));
        }

        VirtualSystem vs = null;
        if (dai != null) {
            vs = dai.getVirtualSystem();
        } else {
            EntityManager<VirtualSystem> vsMgr = new EntityManager<>(VirtualSystem.class, session);
            vs = vsMgr.findByPrimaryKey(request.getVsId());
        }

        // if still not found, must be new. For non-openstack VS'es create the DAI entry
        VirtualizationType virtualizationType = vs.getVirtualizationConnector().getVirtualizationType();
        if (dai == null && !virtualizationType.isOpenstack()) {
            ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(vs);
            AgentType agentType = managerApi.isAgentManaged() ? AgentType.AGENT : AgentType.AGENTLESS;
            dai = new DistributedApplianceInstance(vs, agentType);
            dai.setIpAddress(request.getApplianceIp());
            dai.setName("Temporary" + UUID.randomUUID().toString()); // setting temporary name since it is
            // mandatory field
            dai = EntityManager.create(session, dai);

            // Generate a unique, intuitive and immutable name
            String applianceName = vs.getName() + "-" + dai.getId().toString();
            if (request.getName() != null && !request.getName().isEmpty()) {
                LOG.info("Creating DAI using precreated agent name '" + request.getName() + "'");
                applianceName = request.getName();
            }

            dai.setName(applianceName);
            dai.setPolicyMapOutOfSync(true);
            dai.setPassword(EncryptionUtil.encryptAESCTR(AgentAuthFilter.VMIDC_AGENT_PASS));

            LOG.info("Creating new DAI " + dai);
        } else if (dai == null) {
            // For openstack we pre-create the DAI, so dont create new DAI's for any openstack SVA register requests.
            // This should generally not happen, because we delete the SVA before we delete the DAI
            LOG.warn("Ignoring Agent Register request for Openstack SVA as the SVA/DAI is already deleted from OSC or"
                    + " is still being created");
            return new AgentRegisterServiceResponse();
        }

        updateDaiAgentStatusInfo(request, session, dai);

        DistributedAppliance da = dai.getVirtualSystem().getDistributedAppliance();

        AgentRegisterServiceResponse response = new AgentRegisterServiceResponse();
        response.setApplianceName(dai.getName());
        response.setMgrIp(da.getApplianceManagerConnector().getIpAddress());
        response.setSharedSecretKey(da.getMgrSecretKey());

        // Get manager's appliance instance initial configuration
        ManagerDeviceApi mgrApi = null;
        try {
            mgrApi = ManagerApiFactory.createManagerDeviceApi(vs);
            response.setApplianceConfig1(mgrApi.getDeviceMemberConfiguration(dai));
            response.setApplianceConfig2(mgrApi.getDeviceMemberAdditionalConfiguration(dai));
        } catch (Exception ex) {
            LOG.error("Fail to retrieve appliance instance initial configuration from manager. Better luck next time.",
                    ex);
            AlertGenerator.processDaiFailureEvent(DaiFailureType.DAI_MGR_INITIAL_CONFIG, new LockObjectReference(dai),
                    "Fail to get Manager Initial config for Appliance Instance '" + dai.getName() + "'");
        } finally {
            if (mgrApi != null) {
                mgrApi.close();
            }
        }

        // Check if traffic policy mapping is out of sync
        ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(dai);
        if (dai.isPolicyMapOutOfSync() && managerApi.isSecurityGroupSyncSupport() && managerApi.isAgentManaged()) {
            startDaiAgentPolicyMappingJob(dai);
        }

        // Check if console password is out of sync
        if (dai.getNewConsolePassword() != null) {
            // start job to sync all policies assignments
            startDaiConsolePasswordJob(dai);
        }

        return response;
    }

    public static void updateDaiAgentStatusInfo(AgentRegisterServiceRequest request, Session session,
            DistributedApplianceInstance dai) {

        dai.setAgentVersionMajor(request.getAgentVersion().getMajor());
        dai.setAgentVersionMinor(request.getAgentVersion().getMinor());
        dai.setAgentVersionStr(request.getAgentVersion().getVersionStr());

        // if DAI has no floating ip and the DS does not have floating ip pool assigned, update ip to appliance ip
        if (dai.getFloatingIpId() == null && dai.getDeploymentSpec() != null
                && StringUtils.isEmpty(dai.getDeploymentSpec().getFloatingIpPoolName())) {
            dai.setIpAddress(request.getApplianceIp());
        }
        dai.setMgmtIpAddress(request.getApplianceIp());
        dai.setMgmtGateway(request.getApplianceGateway());
        if (request.getApplianceSubnetMask() != null) {
            dai.setMgmtSubnetPrefixLength(
                    String.valueOf(NetworkUtil.getPrefixLength(request.getApplianceSubnetMask())));
        } else {
            dai.setMgmtSubnetPrefixLength(null);
        }

        // Generate an alert if Appliance Instance Discovery flag changed from 'true' to 'false'
        if (dai.getDiscovered() != null && dai.getDiscovered() && !request.isDiscovered()) {
            LOG.warn("Generate an alert if Appliance Instance Discovery flag changed from 'true' to 'false'");
            AlertGenerator.processDaiFailureEvent(DaiFailureType.DAI_STATUS_CHANGE, new LockObjectReference(dai),
                    "Appliance Instance '" + dai.getName() + "' Discovery flag changed from 'true' to 'false'");
        }
        dai.setDiscovered(request.isDiscovered());
        // Generate an alert if Appliance Instance Inspection Ready flag changed from 'true' to 'false'
        if (dai.getInspectionReady() != null && dai.getInspectionReady() && !request.isInspectionReady()) {
            LOG.warn("Generate an alert if Appliance Instance Inspection Ready flag changed from 'true' to 'false'");
            AlertGenerator.processDaiFailureEvent(DaiFailureType.DAI_STATUS_CHANGE, new LockObjectReference(dai),
                    "Appliance Instance '" + dai.getName() + "' Inspection Ready flag changed from 'true' to 'false'");
        }
        dai.setInspectionReady(request.isInspectionReady());
        dai.setLastStatus(new Date());
        if (request.getAgentDpaInfo() != null && request.getAgentDpaInfo().netXDpaRuntimeInfo != null) {
            dai.setWorkloadInterfaces(request.getAgentDpaInfo().netXDpaRuntimeInfo.workloadInterfaces);
            dai.setPackets(request.getAgentDpaInfo().netXDpaRuntimeInfo.rx);
        }

        if (dai.getVirtualSystem().getVirtualizationConnector().isVmware()) {
            updateNsxAgentInfo(session, dai);
        }
        if (!request.isDiscovered()) {
            try {
                checkMemberDevice(dai, session);
            } catch (Exception e) {
                // If we experience any exceptions during member check/creation
                // we'll note it, but allow registration to complete so DAI will
                // be at list created, allowing the user interactions.
                LOG.error("Appliance registration: failure while checking member device " + e.getMessage());
                AlertGenerator.processDaiFailureEvent(DaiFailureType.DAI_MGR_CHECK, new LockObjectReference(dai),
                        "Fail to check Manager Info for Appliance Instance '" + dai.getName() + "'");
            }
        }

        // Update DAI to reflect last successful communication
        EntityManager.update(session, dai);
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

    private void startDaiConsolePasswordJob(DistributedApplianceInstance dai) throws Exception {
        LOG.info("DAI '" + dai.getName()
        + "' console password was found to be out of sync. Triggering password sych job.");
        TaskGraph tg = new TaskGraph();
        tg.addTask(new UpdateApplianceConsolePasswordTask(dai, dai.getNewConsolePassword()));

        Job job = JobEngine.getEngine().submit(
                "Update out-of-sync Appliance Console Password for DAI : '" + dai.getName() + "'", tg,
                LockObjectReference.getObjectReferences(dai));
        LOG.info("Job " + job.getId() + " submitted.");
    }

    private void startDaiAgentPolicyMappingJob(DistributedApplianceInstance dai) throws Exception {
        LOG.info("DAI '" + dai.getName()
        + "' traffic policy mapping was found to be out of sync. Triggering mapping sych job.");
        TaskGraph tg = new TaskGraph();
        tg.addTask(new AgentInterfaceEndpointMapSetTask(dai));

        Job job = JobEngine.getEngine().submit(
                "Update out-of-sync Appliance Traffic Policy Mapping for DAI : '" + dai.getName() + "'", tg,
                LockObjectReference.getObjectReferences(dai));
        LOG.info("Job " + job.getId() + " submitted.");
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
}
