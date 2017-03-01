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
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.rest.client.nsx.model.Agent;
import org.osc.core.broker.rest.client.nsx.model.Agent.AllocatedIpAddress;
import org.osc.core.broker.rest.client.nsx.model.Agent.HostInfo;
import org.osc.core.broker.service.request.AgentRegisterServiceRequest;
import org.osc.core.rest.client.agent.model.output.AgentDpaInfo;
import org.osc.core.util.VersionUtil;
import org.osc.core.util.encryption.EncryptionException;

class AgentRegisterServiceTestData {
    static String DAI_TEMPORARY_NAME = "Temporary";
    static byte[] DEVICE_CONFIGURATION = new byte[3];
    static byte[] DEVICE_ADDITIONAL_CONFIGURATION = new byte[4];

    static Long OPENSTACK_VS_ID = 1L;
    static Long VMWARE_VS_ID = 2L;
    static Long NO_NSX_AGENT_VS_ID = 3L;
    static Long WITH_NSX_AGENT_VS_ID = 4L;
    static Long MISMATCHING_VS_ID = 5L;

    static String NO_NSX_AGENT_NSX_SERVICE_ID = "NO_NSX_AGENT_NSX_SERVICE_ID";
    static String WITH_NSX_AGENT_NSX_SERVICE_ID = "WITH_NSX_AGENT_NSX_SERVICE_ID";

    static VirtualSystem OPENSTACK_VS = createVirtualSystem(VirtualizationType.OPENSTACK, OPENSTACK_VS_ID, null);
    static VirtualSystem VMWARE_VS = createVirtualSystem(VirtualizationType.VMWARE, VMWARE_VS_ID, null);
    static VirtualSystem NO_NSX_AGENT_VS = createVirtualSystem(VirtualizationType.VMWARE, NO_NSX_AGENT_VS_ID, NO_NSX_AGENT_NSX_SERVICE_ID);
    static VirtualSystem WITH_NSX_AGENT_VS = createVirtualSystem(VirtualizationType.VMWARE, WITH_NSX_AGENT_VS_ID, WITH_NSX_AGENT_NSX_SERVICE_ID);

    static AgentRegisterServiceRequest INVALID_REQUEST = new AgentRegisterServiceRequest();
    static AgentRegisterServiceRequest NULL_DAI_OPENSTACK_REQUEST = createRequest(OPENSTACK_VS_ID, "NULL_DAI_OPENSTACK_IP");

    static AgentRegisterServiceRequest OPENSTACK_MISMATCH_VS_ID_REQUEST = createRequest(MISMATCHING_VS_ID, "OPENSTACK_MISMATCH_VS_ID_IP");
    static DistributedApplianceInstance MISTMATCH_VS_ID_DAI = new DistributedApplianceInstance(OPENSTACK_VS);

    static AgentRegisterServiceRequest NULL_DAI_VMWARE_REQUEST;

    static Long NULL_DAI_VMWARE_DAI_ID = 105L;

    static AgentRegisterServiceRequest EXISTING_DAI_REQUEST;

    static DistributedApplianceInstance EXISTING_DAI;

    static AgentRegisterServiceRequest NO_NSX_AGENT_REQUEST;

    static Long NO_NSX_AGENT_DAI_ID = 305L;

    static AgentRegisterServiceRequest WITH_NSX_AGENT_REQUEST;

    static Long WITH_NSX_AGENT_DAI_ID = 405L;

    static Agent NSX_AGENT;

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
        try {
            NULL_DAI_VMWARE_REQUEST =
                    createRequest(
                            VMWARE_VS_ID,
                            "NULL_DAI_VMWARE_IP",
                            "NULL_DAI_VMWARE_NAME",
                            101L,
                            102L,
                            "NULL_DAI_VMWARE_VERSION",
                            "NULL_DAI_VMWARE_GATEWAY",
                            null,
                            false,
                            false,
                            103L,
                            104L);

            EXISTING_DAI_REQUEST =
                    createRequest(
                            VMWARE_VS_ID,
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

            EXISTING_DAI =
                    createDistributedApplianceInstance(VMWARE_VS, 205L, "EXISTING_DAI_NAME", "EXISTING_DAI_IP");

            NO_NSX_AGENT_REQUEST =
                    createRequest(
                            NO_NSX_AGENT_VS_ID,
                            "NO_NSX_AGENT_IP",
                            "NO_NSX_AGENT_NAME",
                            301L,
                            302L,
                            "NO_NSX_AGENT_VERSION",
                            "NO_NSX_AGENT_GATEWAY",
                            null,
                            false,
                            false,
                            303L,
                            304L);

            WITH_NSX_AGENT_REQUEST =
                    createRequest(
                            WITH_NSX_AGENT_VS_ID,
                            "WITH_NSX_AGENT_IP",
                            "WITH_NSX_AGENT_NAME",
                            401L,
                            402L,
                            "WITH_NSX_AGENT_VERSION",
                            "WITH_NSX_AGENT_GATEWAY",
                            null,
                            true,
                            true,
                            403L,
                            404L);

            DAI_INSPECTION_READY_REQUEST =
                    createRequest(
                            VMWARE_VS_ID,
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

            NSX_AGENT = createAgent(WITH_NSX_AGENT_REQUEST.getApplianceIp());

            DAI_DISCOVERED_REQUEST =
                    createRequest(
                            VMWARE_VS_ID,
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
                            VMWARE_VS_ID,
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
                            VMWARE_VS_ID,
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
                            VMWARE_VS_ID,
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
                            VMWARE_VS_ID,
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

            AGENT_HEALTH_MISMATCH_DAI =
                    createDistributedApplianceInstance(
                            VMWARE_VS,
                            5005L,
                            "AGENT_HEALTH_MISMATCH_NAME",
                            "AGENT_HEALTH_MISMATCH_IP",
                            "AGENT_HEALTH_MISMATCH_NSX_AGENT_ID",
                            true,
                            true);

            AGENT_HEALTH_MISMATCH_DISCOVERED_DAI =
                    createDistributedApplianceInstance(
                            VMWARE_VS,
                            6005L,
                            "AGENT_HEALTH_MISMATCH_DISCOVERED_NAME",
                            "AGENT_HEALTH_MISMATCH_DISCOVERED_IP",
                            "AGENT_HEALTH_MISMATCH_NSX_AGENT_DISCOVERED_ID",
                            true,
                            false);

            AGENT_HEALTH_MISMATCH_NOT_DISCOVERED_NOT_INSPECTIONREADY_DAI =
                    createDistributedApplianceInstance(
                            VMWARE_VS,
                            7005L,
                            "AGENT_HEALTH_MISMATCH_NOT_DISCOVERED_NOT_INSPECTIONREADY_NAME",
                            "AGENT_HEALTH_MISMATCH_NOT_DISCOVERED_NOT_INSPECTIONREADY_IP",
                            "AGENT_HEALTH_MISMATCH_NOT_DISCOVERED_NOT_INSPECTIONREADY_NSX_AGENT_ID",
                            false,
                            false);

            AGENT_HEALTH_MATCH_DAI =
                    createDistributedApplianceInstance(
                            VMWARE_VS,
                            8005L,
                            "AGENT_HEALTH_MATCH_NAME",
                            "AGENT_HEALTH_MATCH_IP",
                            "AGENT_HEALTH_MATCH_NSX_AGENT_ID",
                            true,
                            true);

            NEW_CONSOLE_PASSWORD_DAI =
                    createDistributedApplianceInstance(
                            VMWARE_VS,
                            9005L,
                            "NEW_CONSOLE_PASSWORD_NAME",
                            "NEW_CONSOLE_PASSWORD_IP",
                            null,
                            null,
                            null,
                            "NEW_CONSOLE_PASSWORD_PWD",
                            false,
                            false);

            SEC_GROUP_OUT_OF_SYNC_DAI =
                    createDistributedApplianceInstance(
                            VMWARE_VS,
                            10005L,
                            "SEC_GROUP_OUT_OF_SYNC_NAME",
                            "SEC_GROUP_OUT_OF_SYNC_IP",
                            null,
                            null,
                            null,
                            null,
                            true,
                            true);
        } catch(EncryptionException encryptionException) {
            System.err.println("Failed to initialize test data. Encryption error : " + encryptionException.getStackTrace());
        }
    }

    private static DistributedApplianceInstance createDistributedApplianceInstance(
            VirtualSystem vs,
            Long daiId,
            String daiName,
            String daiIp) throws EncryptionException {
        return createDistributedApplianceInstance(vs, daiId, daiName, daiIp, null, null, null, null, false, false);
    }

    private static DistributedApplianceInstance createDistributedApplianceInstance(
            VirtualSystem vs,
            Long daiId,
            String daiName,
            String daiIp,
            String nsxAgentId,
            Boolean isDiscovered,
            Boolean isInspectionReady) throws EncryptionException {
        return createDistributedApplianceInstance(vs, daiId, daiName, daiIp, nsxAgentId, isDiscovered,isInspectionReady, null, false, false);
    }

    private static DistributedApplianceInstance createDistributedApplianceInstance(
            VirtualSystem vs,
            Long daiId,
            String daiName,
            String daiIp,
            String nsxAgentId,
            Boolean isDiscovered,
            Boolean isInspectionReady,
            String consolePassword,
            boolean policyMapOutOfSync,
            boolean includeDeploymentSpec) throws EncryptionException {
        DistributedApplianceInstance dai =  new DistributedApplianceInstance(vs);
        dai.setId(daiId);
        dai.setName(daiName);
        dai.setIpAddress(daiIp);
        dai.setMgmtGateway("MGMT_GATEWAY");
        dai.setNsxAgentId(nsxAgentId);
        dai.setDiscovered(isDiscovered);
        dai.setInspectionReady(isInspectionReady);
        dai.setNewConsolePassword(consolePassword);
        if (includeDeploymentSpec) {
            DeploymentSpec ds = new DeploymentSpec();
            dai.setDeploymentSpec(ds);
        }

        return dai;
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
        request.setAgentVersion(version);
        request.setApplianceGateway(applianceGateway);
        request.setApplianceSubnetMask(applianceSubnetMask);
        request.setDiscovered(isDiscovered);
        request.setInspectionReady(isInspectionReady);
        request.setAgentDpaInfo(dpaInfo);

        return request;
    }

    private static VirtualSystem createVirtualSystem(VirtualizationType virtualizationType, Long vsId, String nsxServiceId) {
        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setIpAddress("1.1.1.1");
        mc.setPublicKey(new byte[3]);
        mc.setManagerType(ManagerType.NSM);

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
        vs.setNsxServiceId(nsxServiceId);

        return vs;
    }

    private static Agent createAgent(String ipAddress) {
        AllocatedIpAddress allocatedIp = new AllocatedIpAddress();
        allocatedIp.ipAddress = ipAddress;
        allocatedIp.gateway = "GATEWAY";
        allocatedIp.prefixLength = "LENGTH";

        HostInfo hostInfo = new HostInfo();
        hostInfo.objectId = "HOST_INFO_OID";
        hostInfo.name = "HOST_INFO_NAME";
        hostInfo.vsmUuid = "HOST_INFO_VMUIID";

        Agent agent = new Agent();
        agent.allocatedIpAddress = allocatedIp;
        agent.hostInfo = hostInfo;
        agent.agentId = "AGENT_ID";
        agent.vmId = "VM_ID";

        return agent;
    }
}
