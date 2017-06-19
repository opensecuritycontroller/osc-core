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

import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.common.manager.ManagerType;
import org.osc.core.broker.service.request.AgentRegisterServiceRequest;
import org.osc.core.broker.service.response.AgentDpaInfo;
import org.osc.core.broker.util.VersionUtil;

class AgentRegisterServiceTestData {
    static String DAI_TEMPORARY_NAME = "Temporary";
    static byte[] DEVICE_CONFIGURATION = new byte[3];
    static byte[] DEVICE_ADDITIONAL_CONFIGURATION = new byte[4];

    static Long OPENSTACK_VS_ID = 1L;
    static Long MISMATCHING_VS_ID = 5L;

    static VirtualSystem OPENSTACK_VS = createVirtualSystem(VirtualizationType.OPENSTACK, OPENSTACK_VS_ID);

    static AgentRegisterServiceRequest INVALID_REQUEST = new AgentRegisterServiceRequest();
    static AgentRegisterServiceRequest NULL_DAI_OPENSTACK_REQUEST = createRequest(OPENSTACK_VS_ID, "NULL_DAI_OPENSTACK_IP");

    static AgentRegisterServiceRequest OPENSTACK_MISMATCH_VS_ID_REQUEST = createRequest(MISMATCHING_VS_ID, "OPENSTACK_MISMATCH_VS_ID_IP");
    static DistributedApplianceInstance MISTMATCH_VS_ID_DAI = new DistributedApplianceInstance(OPENSTACK_VS);

    static AgentRegisterServiceRequest EXISTING_DAI_REQUEST;

    static DistributedApplianceInstance EXISTING_DAI;

    static AgentRegisterServiceRequest DAI_INSPECTION_READY_REQUEST;

    static AgentRegisterServiceRequest DAI_DISCOVERED_REQUEST;

    static AgentRegisterServiceRequest DAI_NOT_DISCOVERED_NOT_READY_REQUEST;

    static AgentRegisterServiceRequest DAI_AGENT_HEALTH_MATCH_REQUEST;

    static AgentRegisterServiceRequest NEW_CONSOLE_PASSWORD_REQUEST;

    static AgentRegisterServiceRequest SEC_GROUP_OUT_OF_SYNC_REQUEST;

    static DistributedApplianceInstance AGENT_HEALTH_MISMATCH_DAI;

    static DistributedApplianceInstance AGENT_HEALTH_MISMATCH_DISCOVERED_DAI;

    static DistributedApplianceInstance AGENT_HEALTH_MISMATCH_NOT_DISCOVERED_NOT_INSPECTIONREADY_DAI;

    static DistributedApplianceInstance AGENT_HEALTH_MATCH_DAI;

    static DistributedApplianceInstance NEW_CONSOLE_PASSWORD_DAI;

    static DistributedApplianceInstance SEC_GROUP_OUT_OF_SYNC_DAI;

    static {
        EXISTING_DAI_REQUEST =
                createRequest(
                        OPENSTACK_VS_ID,
                        "EXISTING_DAI_IP",
                        "EXISTING_DAI_NAME",
                        201L,
                        202L,
                        "EXISTING_DAI_VERSION",
                        "EXISTING_DAI_GATEWAY",
                        null,
                        false,
                        false,
                        203L,
                        204L);


        DAI_INSPECTION_READY_REQUEST =
                createRequest(
                        OPENSTACK_VS_ID,
                        "DAI_INSPECTION_READY_IP",
                        "DAI_INSPECTION_READY_NAME",
                        501L,
                        502L,
                        "DAI_INSPECTION_READY_VERSION",
                        "DAI_INSPECTION_READY_GATEWAY",
                        null,
                        true,
                        true,
                        503L,
                        504L);

        DAI_DISCOVERED_REQUEST =
                createRequest(
                        OPENSTACK_VS_ID,
                        "DAI_DISCOVERED_IP",
                        "DAI_DISCOVERED_NAME",
                        601L,
                        602L,
                        "DAI_DISCOVERED_VERSION",
                        "DAI_DISCOVERED_GATEWAY",
                        null,
                        true,
                        false,
                        603L,
                        604L);

        DAI_NOT_DISCOVERED_NOT_READY_REQUEST =
                createRequest(
                        OPENSTACK_VS_ID,
                        "DAI_NOT_DISCOVERED_NOT_READY_IP",
                        "DAI_DISCOVERED_NAME",
                        701L,
                        702L,
                        "DAI_NOT_DISCOVERED_NOT_READY_VERSION",
                        "DAI_NOT_DISCOVERED_NOT_READY_GATEWAY",
                        null,
                        false,
                        false,
                        703L,
                        704L);

        DAI_AGENT_HEALTH_MATCH_REQUEST =
                createRequest(
                        OPENSTACK_VS_ID,
                        "DAI_AGENT_HEALTH_MATCH_IP",
                        "DAI_AGENT_HEALTH_MATCH_NAME",
                        801L,
                        802L,
                        "DAI_AGENT_HEALTH_MATCH_VERSION",
                        "DAI_AGENT_HEALTH_MATCH_GATEWAY",
                        null,
                        true,
                        true,
                        803L,
                        804L);

        NEW_CONSOLE_PASSWORD_REQUEST =
                createRequest(
                        OPENSTACK_VS_ID,
                        "NEW_CONSOLE_PASSWORD_IP",
                        "NEW_CONSOLE_PASSWORD_NAME",
                        901L,
                        902L,
                        "NEW_CONSOLE_PASSWORD_VERSION",
                        "NEW_CONSOLE_PASSWORD_GATEWAY",
                        "255.255.255.0",
                        true,
                        true,
                        903L,
                        904L);

        SEC_GROUP_OUT_OF_SYNC_REQUEST =
                createRequest(
                        OPENSTACK_VS_ID,
                        "SEC_GROUP_OUT_OF_SYNC_REQUEST_IP",
                        "SEC_GROUP_OUT_OF_SYNC_REQUEST_NAME",
                        1001L,
                        1002L,
                        "SEC_GROUP_OUT_OF_SYNC_REQUEST_VERSION",
                        "SSEC_GROUP_OUT_OF_SYNC_REQUEST_GATEWAY",
                        null,
                        true,
                        true,
                        1003L,
                        1004L);
}


    private static AgentRegisterServiceRequest createRequest(Long vsId, String applianceIp) {
        return createRequest(
                vsId,
                applianceIp,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                null);
    }

    private static AgentRegisterServiceRequest createRequest(
            Long vsId,
            String applianceIp,
            String applianceName,
            Long majorVersion,
            Long minorVersion,
            String versionStr,
            String applianceGateway,
            String applianceSubnetMask,
            boolean isDiscovered,
            boolean isInspectionReady,
            Long dpaRuntimeWorkloadInterfaces,
            Long dapRuntimeRx) {
        VersionUtil.Version version = new VersionUtil.Version(majorVersion, minorVersion, null);
        version.setVersionStr(versionStr);
        AgentDpaInfo.NetXDpaRuntimeInfo dpaRuntimeInfo = new AgentDpaInfo.NetXDpaRuntimeInfo();
        dpaRuntimeInfo.workloadInterfaces = dpaRuntimeWorkloadInterfaces;
        dpaRuntimeInfo.rx = dapRuntimeRx;

        AgentDpaInfo dpaInfo = new AgentDpaInfo();
        dpaInfo.netXDpaRuntimeInfo = dpaRuntimeInfo;

        AgentRegisterServiceRequest request = new AgentRegisterServiceRequest();
        request.setVirtualSystemId(vsId);
        request.setApplianceIp(applianceIp);
        request.setName(applianceName);
        request.setAgentVersion(version.getVersionStr());
        request.setApplianceGateway(applianceGateway);
        request.setApplianceSubnetMask(applianceSubnetMask);
        request.setDiscovered(isDiscovered);
        request.setInspectionReady(isInspectionReady);
        request.setAgentDpaInfo(dpaInfo);

        return request;
    }

    private static VirtualSystem createVirtualSystem(VirtualizationType virtualizationType, Long vsId) {
        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setIpAddress("1.1.1.1");
        mc.setPublicKey(new byte[3]);
        mc.setManagerType(ManagerType.NSM.getValue());

        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setVirtualizationType(virtualizationType);

        DistributedAppliance da = new DistributedAppliance(mc);
        da.setId(5000L);
        da.setName("DA_NAME");
        da.setMgrSecretKey("MGR_SECRET_KEY");

        VirtualSystem vs = new VirtualSystem(da);
        vs.setVirtualizationConnector(vc);
        vs.setId(vsId);
        vs.setKeyStore(new byte[3]);

        return vs;
    }
}
