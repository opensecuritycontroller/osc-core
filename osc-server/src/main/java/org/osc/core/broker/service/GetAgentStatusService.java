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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.events.DaiFailureType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.DistributedApplianceInstanceElementImpl;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.DistributedApplianceInstancesRequest;
import org.osc.core.broker.service.response.GetAgentStatusResponseDto;
import org.osc.core.rest.client.agent.model.output.AgentDpaInfo;
import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;
import org.osc.core.util.VersionUtil;
import org.osc.sdk.manager.api.ManagerDeviceMemberApi;
import org.osc.sdk.manager.element.DistributedApplianceInstanceElement;
import org.osc.sdk.manager.element.ManagerDeviceMemberStatusElement;

public class GetAgentStatusService extends ServiceDispatcher<DistributedApplianceInstancesRequest, GetAgentStatusResponseDto> {

    private static final Logger LOG = Logger.getLogger(GetAgentStatusService.class);
    private List<DistributedApplianceInstance> daiList = null;
    private Session session = null;

    @Override
    public GetAgentStatusResponseDto exec(DistributedApplianceInstancesRequest request, Session session) throws Exception {
        this.session = session;
        DistributedApplianceInstancesRequest.checkForNullFields(request);

        GetAgentStatusResponseDto response = new GetAgentStatusResponseDto();

        this.daiList =  DistributedApplianceInstanceEntityMgr.getByIds(session, request.getDtoIdList());

        List<AgentStatusResponse> responses = getApplianceInstanceStatus(this.daiList);

        response.setAgentStatusList(responses);

        return response;
    }

    private List<AgentStatusResponse> getApplianceInstanceStatus(
            final Collection<DistributedApplianceInstance> agentlessDais) throws Exception {
        List<AgentStatusResponse> agentStatusResponses = new ArrayList<>();
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

            List<DistributedApplianceInstance> list = entry.getValue();
            if (ManagerApiFactory.providesDeviceStatus(vs)) {
                ApplianceManagerConnector apmc = vs.getDistributedAppliance().getApplianceManagerConnector();
                ManagerDeviceMemberApi agentApi =  ManagerApiFactory.createManagerDeviceMemberApi(apmc, vs);

                List<DistributedApplianceInstanceElement> elements = list.stream()
                        .map(DistributedApplianceInstanceElementImpl::new)
                        .collect(Collectors.toList());

                agentStatusResponses.addAll(invokeRequest(elements, agentApi));
            } else {
                agentStatusResponses.addAll(createStatusNotSupportedResponses(list));
            }
        }

        return agentStatusResponses;
    }

    private List<AgentStatusResponse> createStatusNotSupportedResponses(List<DistributedApplianceInstance> dais)  {
        List<AgentStatusResponse> responses = new ArrayList<AgentStatusResponse>();
        for (DistributedApplianceInstance dai : dais) {
            AgentStatusResponse status = new AgentStatusResponse();
            status.setApplianceId(dai.getId());
            status.setApplianceName(dai.getName());
            status.setApplianceIp(dai.getIpAddress());
            status.setPublicIp(dai.getIpAddress());
            status.setVirtualServer(dai.getHostName());
            status.setStatusLines(Arrays.asList("The security manager for this appliance instance does not provide appliance status."));
            responses.add(status);
        }

        return responses;
    }

    private List<AgentStatusResponse> invokeRequest(List<DistributedApplianceInstanceElement> dais,
            final ManagerDeviceMemberApi agentApi) {
        List<AgentStatusResponse> agentStatusList = new ArrayList<>();
        try {
            List<ManagerDeviceMemberStatusElement> agentElems = agentApi.getFullStatus(dais);
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

    private static void updateDaiAgentStatusInfo(Session session, AgentStatusResponse agentStatus, DistributedApplianceInstance dai) {
        // Generate an alert if Appliance Instance Discovery flag changed from 'true' to 'false'
        if (dai.getDiscovered() != null && dai.getDiscovered() && !agentStatus.isDiscovered()) {
            LOG.warn("Generate an alert if Appliance Instance Discovery flag changed from 'true' to 'false'");
            AlertGenerator.processDaiFailureEvent(DaiFailureType.DAI_STATUS_CHANGE, new LockObjectReference(dai),
                    "Appliance Instance '" + dai.getName() + "' Discovery flag changed from 'true' to 'false'");
        }

        dai.setDiscovered(agentStatus.isDiscovered());
        // Generate an alert if Appliance Instance Inspection Ready flag changed from 'true' to 'false'
        if (dai.getInspectionReady() != null && dai.getInspectionReady() && !agentStatus.isInspectionReady()) {
            LOG.warn("Generate an alert if Appliance Instance Inspection Ready flag changed from 'true' to 'false'");
            AlertGenerator.processDaiFailureEvent(DaiFailureType.DAI_STATUS_CHANGE, new LockObjectReference(dai),
                    "Appliance Instance '" + dai.getName() + "' Inspection Ready flag changed from 'true' to 'false'");
        }

        dai.setInspectionReady(agentStatus.isInspectionReady());
        dai.setLastStatus(new Date());
        if (agentStatus.getAgentDpaInfo() != null && agentStatus.getAgentDpaInfo().netXDpaRuntimeInfo != null) {
            dai.setWorkloadInterfaces(agentStatus.getAgentDpaInfo().netXDpaRuntimeInfo.workloadInterfaces);
            dai.setPackets(agentStatus.getAgentDpaInfo().netXDpaRuntimeInfo.rx);
        }

        if (dai.getVirtualSystem().getVirtualizationConnector().getVirtualizationType() == VirtualizationType.VMWARE) {
            NsxUpdateAgentsService.updateNsxAgentInfo(session, dai);
        }

        // Update DAI to reflect last successful communication
        EntityManager.update(session, dai);
    }
}
