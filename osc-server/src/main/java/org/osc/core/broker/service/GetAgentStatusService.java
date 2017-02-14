package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.AgentType;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.events.DaiFailureType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.AgentRegisterServiceRequest;
import org.osc.core.broker.service.request.DistributedApplianceInstancesRequest;
import org.osc.core.broker.service.response.AgentStatusResponseDto;
import org.osc.core.broker.service.response.GetAgentStatusResponseDto;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;
import org.osc.core.rest.client.agent.model.output.AgentDpaInfo;
import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;
import org.osc.core.util.NetworkUtil;
import org.osc.core.util.VersionUtil;
import org.osc.sdk.manager.api.ManagerDeviceMemberApi;
import org.osc.sdk.manager.element.DistributedApplianceInstanceElement;
import org.osc.sdk.manager.element.ManagerDeviceMemberStatusElement;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.mcafee.vmidc.server.Server;

public class GetAgentStatusService extends ServiceDispatcher<DistributedApplianceInstancesRequest, GetAgentStatusResponseDto> {

    private static final Logger LOG = Logger.getLogger(GetAgentStatusService.class);
    private List<DistributedApplianceInstance> daiList = null;
    private Session session = null;

    @Override
    public GetAgentStatusResponseDto exec(DistributedApplianceInstancesRequest request, Session session) throws Exception {
        this.session = session;
        DistributedApplianceInstancesRequest.checkForNullFields(request);

        GetAgentStatusResponseDto response = new GetAgentStatusResponseDto();
        List<AgentStatusResponseDto> statusList = new ArrayList<>();

        this.daiList =  DistributedApplianceInstanceEntityMgr.getByIds(session, request.getDtoIdList());

        Collection<DistributedApplianceInstance> agentlessDais = ValidateUtil.filterDistributedApplInstances(
                this.daiList, AgentType.AGENT);
        Collection<DistributedApplianceInstance> agentDais = ValidateUtil.filterDistributedApplInstances(
                this.daiList, AgentType.AGENTLESS);
        AgentStatusResponse resp = null;
        if (agentlessDais.size() == 0) {
            //all are agent based
            for (DistributedApplianceInstance dai : agentDais) {
                resp = getAgentStatus(dai);
                statusList.add(new AgentStatusResponseDto(resp, AgentType.AGENT.toString()));
            }
        } else if (this.daiList.size() != agentlessDais.size()){
            //mix
            Collection<AgentStatusResponse> responses = getAgentlessStatus(agentlessDais);
            Collection<AgentStatusResponseDto> results = transformToDto(responses, AgentType.AGENTLESS);
            statusList.addAll(results);
            for (DistributedApplianceInstance dai : agentDais) {
                resp = getAgentStatus(dai);
                statusList.add(new AgentStatusResponseDto(resp, AgentType.AGENT.toString()));
            }
        } else if (this.daiList.size() == agentlessDais.size()){
            //all are agentless
            Collection<AgentStatusResponse> responses = getAgentlessStatus(agentlessDais);
            Collection<AgentStatusResponseDto> results = transformToDto(responses, AgentType.AGENTLESS);
            statusList.addAll(results);
        }
        response.setAgentStatusList(statusList);
        return response;
    }

    private Collection<AgentStatusResponseDto> transformToDto(Collection<AgentStatusResponse> responses,
            final AgentType agentType) {
        Collection<AgentStatusResponseDto> results = Collections2.transform(responses,
                new Function<AgentStatusResponse, AgentStatusResponseDto>(){
            @Override
            public AgentStatusResponseDto apply(AgentStatusResponse input) {
                return new AgentStatusResponseDto(input, agentType.toString());
            }
        });
        return results;
    }

    private Collection<AgentStatusResponse> getAgentlessStatus(
            final Collection<DistributedApplianceInstance> agentlessDais)
                    throws Exception {
        Collection<AgentStatusResponse> agentStatusResponses = new ArrayList<>();
        HashMap<VirtualSystem, List<DistributedApplianceInstance>> map = new HashMap<>();
        for(DistributedApplianceInstance dai: agentlessDais){
            VirtualSystem vs = dai.getVirtualSystem();
            List<DistributedApplianceInstance> listDAI = map.get(vs);
            if (listDAI == null) {
                listDAI = new ArrayList<>();
                map.put(vs, listDAI);
            }
            listDAI.add(dai);
        }

        for (Entry<VirtualSystem, List<DistributedApplianceInstance>> entry : map.entrySet()){
            VirtualSystem vs = entry.getKey();
            ApplianceManagerConnector apmc = vs.getDistributedAppliance().getApplianceManagerConnector();
            ManagerDeviceMemberApi agentApi =  ManagerApiFactory.createManagerDeviceMemberApi(apmc, vs);
            List<DistributedApplianceInstance> list = entry.getValue();
            agentStatusResponses.addAll(invokeRequest(new ArrayList<DistributedApplianceInstanceElement>(list), agentApi));
        }
        return agentStatusResponses;
    }

    private Collection<AgentStatusResponse> invokeRequest(List<DistributedApplianceInstanceElement> agentlessDais,
            final ManagerDeviceMemberApi agentApi) {
        List<AgentStatusResponse> agentStatusList = new ArrayList<>();
        try {
            List<ManagerDeviceMemberStatusElement> agentElems = agentApi.getFullStatus(agentlessDais);
            handleResponse(this.session, agentStatusList, agentElems);
        } catch (Exception e) {
            LOG.error("Fail to retrieve agent info", e);
        }
        return agentStatusList;
    }

    private static void handleResponse(Session session, List<AgentStatusResponse> agentStatusList, List<ManagerDeviceMemberStatusElement> agentElems) {
        for (ManagerDeviceMemberStatusElement agentElem : agentElems){
            AgentStatusResponse agentStatus = new AgentStatusResponse();
            VersionUtil.Version version = new VersionUtil.Version();
            version.setVersionStr(agentElem.getVersion());
            agentStatus.setVersion(version);

            agentStatus.setApplianceId(agentElem.getDistributedApplianceInstanceElement().getId());
            agentStatus.setApplianceName(agentElem.getDistributedApplianceInstanceElement().getName());
            agentStatus.setApplianceIp(agentElem.getApplianceIp());
            agentStatus.setManagerIp(agentElem.getManagerIp());
            agentStatus.setApplianceGateway(agentElem.getApplianceGateway());
            agentStatus.setDiscovered(agentElem.isDiscovered().booleanValue());
            agentStatus.setInspectionReady(agentElem.isInspectionReady().booleanValue());
            AgentDpaInfo agentDpaInfo = new AgentDpaInfo();
            agentDpaInfo.netXDpaRuntimeInfo.rx = agentElem.getRx();
            agentDpaInfo.netXDpaRuntimeInfo.txSva = agentElem.getTxSva();
            agentDpaInfo.netXDpaRuntimeInfo.dropSva = agentElem.getDropSva();
            agentStatus.setAgentDpaInfo(agentDpaInfo);
            agentStatus.setCurrentServerTime(agentElem.getCurrentServerTime());
            agentStatus.setPublicIp(agentElem.getPublicIp());
            agentStatus.setBrokerIp(agentElem.getBrokerIp());

            DistributedApplianceInstance dai = (DistributedApplianceInstance) session.get(DistributedApplianceInstance.class,
                    agentElem.getDistributedApplianceInstanceElement().getId(),
                    new LockOptions(LockMode.PESSIMISTIC_WRITE));

            updateDaiAgentStatusInfo(session, agentStatus, dai);
            //  Returns the host name for VMware and Openstack, Virtualization type.
            agentStatus.setVirtualServer(dai.getHostName());
            agentStatus.setPublicIp(dai.getIpAddress());

            agentStatusList.add(agentStatus);
        }

    }



    private AgentStatusResponse getAgentStatus(final DistributedApplianceInstance di) {
        AgentStatusResponse agentStatus = new AgentStatusResponse();
        agentStatus.setApplianceId(di.getId());
        agentStatus.setApplianceName(di.getName());
        try {
            VmidcAgentApi agentApi = new VmidcAgentApi(di.getIpAddress(), Server.getApiPort(),
                    AgentAuthFilter.VMIDC_AGENT_LOGIN, AgentAuthFilter.VMIDC_AGENT_PASS);
            agentStatus = agentApi.getFullStatus();
            DistributedApplianceInstance dai = (DistributedApplianceInstance) this.session.get(
                    DistributedApplianceInstance.class, di.getId(), new LockOptions(LockMode.PESSIMISTIC_WRITE));
            updateDaiAgentStatusInfo(this.session, agentStatus, dai);
            //Returns the host name for VMware and Openstack, Virtualization type.
            agentStatus.setVirtualServer(di.getHostName());
        } catch (Exception e) {
            LOG.error("Fail to retrieve agent info", e);
        }
        agentStatus.setPublicIp(di.getIpAddress());
        return agentStatus;

    }

    private static void updateDaiAgentStatusInfo(Session session, AgentStatusResponse agentStatus,
            DistributedApplianceInstance dai) {
        AgentRegisterServiceRequest request = new AgentRegisterServiceRequest();

        request.setAgentVersion(agentStatus.getVersion());
        request.setDiscovered(agentStatus.isDiscovered());
        request.setInspectionReady(agentStatus.isInspectionReady());
        request.setAgentDpaInfo(agentStatus.getAgentDpaInfo());
        request.setCpaPid(agentStatus.getCpaPid());
        request.setCpaUptime(agentStatus.getCpaUptime());
        request.setCurrentServerTime(agentStatus.getCurrentServerTime());
        request.setApplianceIp(dai.getIpAddress());
        request.setName(dai.getName());
        request.setVirtualSystemId(dai.getVirtualSystem().getId());

        updateDaiAgentStatusInfo(request, session, dai);
    }

    private static void updateDaiAgentStatusInfo(AgentRegisterServiceRequest request, Session session,
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
            NsxUpdateAgentsService.updateNsxAgentInfo(session, dai);
        }

        // Update DAI to reflect last successful communication
        EntityManager.update(session, dai);
    }
}
