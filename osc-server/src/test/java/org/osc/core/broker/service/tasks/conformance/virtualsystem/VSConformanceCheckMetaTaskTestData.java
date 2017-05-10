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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mockito.Mockito;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.appliance.VmwareSoftwareVersion;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.request.Service;
import org.osc.core.broker.service.request.ServiceProfile;
import org.osc.core.broker.service.tasks.FailedWithObjectInfoTask;
import org.osc.core.broker.service.tasks.conformance.GenerateVSSKeysTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteServiceInstanceTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteServiceManagerTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteServiceTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteVsFromDbTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.UnregisterServiceManagerCallbackTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrDeleteVSSDeviceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteFlavorTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteImageFromGlanceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DSConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.NsxSecurityGroupsCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.NsxSecurityGroupInterfacesCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.SecurityGroupCleanupCheckMetaTask;
import org.osc.core.broker.service.tasks.network.UpdateNsxServiceInstanceAttributesTask;
import org.osc.core.broker.service.tasks.network.UpdateNsxServiceManagerTask;
import org.osc.core.broker.service.tasks.passwordchange.UpdateNsxServiceAttributesTask;

public class VSConformanceCheckMetaTaskTestData {

    public static String DEFAULT_SERVICE_NAME = "DEFAULT_SERVICE_NAME";

    public static List<VirtualSystem> TEST_VIRTUAL_SYSTEMS;

    public static VirtualSystem UPDATE_VMWARE_SERVICEMANAGER_NAME_OUT_OF_SYNC_VS =
            createServiceManagerOutOfSyncData(
                    1L,
                    100L,
                    101L,
                    "SERVICEMANAGER_NAME_OUT_OF_SYNC_SM_ID");

    public static VirtualSystem UPDATE_VMWARE_SERVICEMANAGER_URL_OUT_OF_SYNC_VS =
            createServiceManagerOutOfSyncData(
                    2L,
                    200L,
                    201L,
                    "SERVICEMANAGER_URL_OUT_OF_SYNC_SM_ID");

    public static VirtualSystem UPDATE_VMWARE_SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_VS =
            createServiceManagerOutOfSyncData(
                    3L,
                    300L,
                    301L,
                    "SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_SM_ID");

    public static VirtualSystem UPDATE_VMWARE_SERVICE_NAME_OUT_OF_SYNC_VS =
            createServiceOutOfSyncData(
                    4L,
                    400L,
                    401L,
                    "SERVICE_NAME_OUT_OF_SYNC_S_ID");

    public static VirtualSystem UPDATE_VMWARE_SERVICE_IP_OUT_OF_SYNC_VS =
            createServiceOutOfSyncData(
                    5L,
                    500L,
                    501L,
                    "SERVICE_IP_OUT_OF_SYNC_S_ID");

    public static VirtualSystem UPDATE_VMWARE_SERVICE_PASSWORD_OUT_OF_SYNC_VS =
            createServiceOutOfSyncData(
                    6L,
                    600L,
                    601L,
                    "SERVICE_PASSWORD_OUT_OF_SYNC_S_ID");

    public static VirtualSystem UPDATE_VMWARE_SERVICEINSTANCE_IP_OUT_OF_SYNC_VS =
            createServiceInstanceOutOfSyncData(
                    7L,
                    700L,
                    701L,
                    "SERVICE_IP_OUT_OF_SYNC_SI_ID");

    public static VirtualSystem UPDATE_VMWARE_SERVICEINSTANCE_PASSWORD_OUT_OF_SYNC_VS =
            createServiceInstanceOutOfSyncData(
                    8L,
                    800L,
                    801L,
                    "SERVICE_PASSWORD_OUT_OF_SYNC_SI_ID");

    public static VirtualSystem UPDATE_VMWARE_VSPOLICY_MARKED_FOR_DELETION_VS =
            createVsPolicyMarkedForDeletionData(
                    10L,
                    1000L,
                    1001L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "VSPOLICY_MARKED_FOR_DELETION",
                    true,
                    null);

    public static VirtualSystem UPDATE_VMWARE_VSPOLICY_WITHOUT_TEMPLATE_VS =
            createVsPolicyWithoutTemplateData(
                    11L,
                    1100L,
                    1101L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "VSPOLICY_WITHOUT_TEMPLATE",
                    false,
                    null);

    public static VirtualSystem UPDATE_VMWARE_VSDOMAIN_POLICY_VS =
            createDomainPolicyOnlyData(
                    13L,
                    1300L,
                    1301L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "VSDOMAIN_POLICY_VS",
                    null,
                    null);

    public static VirtualSystem UPDATE_VMWARE_VSPOLICY_WITHOUT_SERVICE_ID_VS =
            createVsPolicyWithoutServiceIdData(
                    14L,
                    1400L,
                    1401L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "VSPOLICY_WITHOUT_SERVICE_ID",
                    false,
                    "VSPOLICY_WITHOUT_SERVICE_VTID");

    public static VirtualSystem UPDATE_VMWARE_VSPOLICY_NAME_OUT_OF_SYNC_VS =
            createVsPolicyNameOutOfSyncData(
                    15L,
                    1500L,
                    1501L,
                    null,
                    "VSPOLICY_WITH_SERVICE_ID_SID",
                    null,
                    null,
                    null,
                    "VSPOLICY_WITH_SERVICE_ID",
                    false,
                    "VSPOLICY_WITH_SERVICE_VTID");

    public static VirtualSystem UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_VS =
            createOpenstackNoDeploymentSpecData(
                    16L,
                    1600L,
                    1601L);

    public static VirtualSystem UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS =
            createOpenstackWithDeploymentSpecData(
                    17L,
                    1700L,
                    1701L);

    public static UnlockObjectMetaTask UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_TASK;

    public static VirtualSystem UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS =
            createOpenstacWhenLockingDeploymentSpecFailsData(
                    18L,
                    1800L,
                    1801L);

    public static VirtualSystem DELETE_VMWARE_DELETE_SERVICE_INSTANCE_VS =
            createDeleteServiceInstanceData(
                    19L,
                    1900L,
                    1901L,
                    "DELETE_VMWARE_DELETE_SERVICE_INSTANCE_SI_ID");

    public static VirtualSystem DELETE_VMWARE_DELETE_SERVICE_MANAGER_VS =
            createDeleteServiceManagerData(
                    20L,
                    2000L,
                    2001L,
                    "DELETE_VMWARE_DELETE_SERVICE_MANAGER_SM_ID");

    public static VirtualSystem DELETE_VMWARE_DELETE_SERVICE_VS =
            createDeleteServiceData(
                    21L,
                    2100L,
                    2101L,
                    "DELETE_VMWARE_DELETE_SERVICE_S_ID");

    public static VirtualSystem DELETE_VMWARE_UNDENPLOY_SERVICE_INSTANCE_VS =
            createUndenployServiceInstanceData(
                    22L,
                    2200L,
                    2201L,
                    "DELETE_VMWARE_UNDENPLOY_SERVICE_INSTANCE_S_ID",
                    "DELETE_VMWARE_UNDENPLOY_SERVICE_INSTANCE_SI_ID");

    public static VirtualSystem DELETE_OPENSTACK_WITH_DEPLOYMENT_SPECS_VS =
            createDeleteOpenStackWithDeploymentSpecData(
                    23L,
                    2300L,
                    2301L);

    public static VirtualSystem DELETE_OPENSTACK_WITH_OS_IMAGE_REF_VS =
            createDeleteOpenStackWithOSImageRefData(
                    24L,
                    2400L,
                    2401L);

    public static VirtualSystem DELETE_OPENSTACK_WITH_OS_FLAVOR_REF_VS =
            createDeleteOpenStackWithOSFlavorRefData(
                    25L,
                    2500L,
                    2501L);

    private static VirtualSystem createVirtualSystem(Long vsId, Long vcId, Long daId, String serviceManagerId, String serviceId, String serviceInstanceId) {
        return createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId, null, null);
    }

    private static VirtualSystem createVirtualSystem(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds) {
        return createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId, asvImageUrl, deploymentSpecIds, null, null, null);
    }

    private static VirtualSystem createVirtualSystem(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds,
            String policyName,
            Boolean policyDeletion,
            String vendorTemplateId) {

        // Mock SslContext
        VirtualizationConnector vcSpy = Mockito.spy(VirtualizationConnector.class);

        vcSpy.setVirtualizationType(VirtualizationType.VMWARE);
        vcSpy.setId(vcId);

        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setManagerType(ManagerType.NSM.getValue());

        DistributedAppliance da = new DistributedAppliance(mc);
        da.setId(daId);
        da.setName(DEFAULT_SERVICE_NAME);

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion();
        asv.setImageUrl(asvImageUrl);

        VirtualSystem vs = new VirtualSystem(da);
        vs.setId(vsId);
        vs.setNsxServiceManagerId(serviceManagerId);
        vs.setNsxServiceId(serviceId);
        vs.setVirtualizationConnector(vcSpy);
        vs.setDomain(new Domain());
        vs.setNsxServiceInstanceId(serviceInstanceId);
        vs.setNsxDeploymentSpecIds(deploymentSpecIds);
        vs.setApplianceSoftwareVersion(asv);
        vs.setDomain(new Domain());

        if (policyName != null) {
            Policy policy = new Policy();
            policy.setName(policyName);

            if (policyDeletion != null) {
                policy.setMarkedForDeletion(policyDeletion);

                VirtualSystemPolicy vsp = new VirtualSystemPolicy();
                vsp.setPolicy(policy);
                vsp.setMarkedForDeletion(policyDeletion);
                vsp.setNsxVendorTemplateId(vendorTemplateId);
                vs.addVirtualSystemPolicy(vsp);
            }

            vs.getDomain().addPolicy(policy);
        }

        if (TEST_VIRTUAL_SYSTEMS == null) {
            TEST_VIRTUAL_SYSTEMS = new ArrayList<VirtualSystem>();
        }

        TEST_VIRTUAL_SYSTEMS.add(vs);
        return vs;
    }

    private static VirtualSystem createServiceManagerOutOfSyncData(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, serviceManagerId, null, null);
        return vs;
    }

    public static TaskGraph createServiceManagerOutOfSyncGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(updateNsxServiceManagerTask.create(vs));
        expectedGraph.appendTask(createNsxServiceTask.create(vs));
        expectedGraph.appendTask(nsxDeploymentSpecCheckMetaTask.create(vs, false));
        expectedGraph.appendTask(new RegisterServiceInstanceTask(vs));
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    private static VirtualSystem createServiceOutOfSyncData(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, serviceId, null);
        return vs;
    }

    static ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);

    private static CreateNsxServiceManagerTask createNsxServiceManagerTask;

    private static CreateNsxServiceTask createNsxServiceTask;

    private static UpdateNsxServiceManagerTask updateNsxServiceManagerTask;

    private static NsxDeploymentSpecCheckMetaTask nsxDeploymentSpecCheckMetaTask;

    private static UpdateNsxServiceAttributesTask updateNsxServiceAttributesTask;

    private static UpdateNsxServiceInstanceAttributesTask updateNsxServiceInstanceAttributesTask;

    static {
        createNsxServiceManagerTask = new CreateNsxServiceManagerTask();
        createNsxServiceManagerTask.apiFactoryService = apiFactoryService;

        createNsxServiceTask = new CreateNsxServiceTask();
        createNsxServiceTask.passwordUtil = null;

        updateNsxServiceManagerTask = new UpdateNsxServiceManagerTask();
        updateNsxServiceManagerTask.apiFactoryService = apiFactoryService;

        nsxDeploymentSpecCheckMetaTask = new NsxDeploymentSpecCheckMetaTask();
        nsxDeploymentSpecCheckMetaTask.updateNsxServiceAttributesTask = null;

        updateNsxServiceAttributesTask = new UpdateNsxServiceAttributesTask();
        updateNsxServiceAttributesTask.passwordUtil = null;

        updateNsxServiceInstanceAttributesTask = new UpdateNsxServiceInstanceAttributesTask();
        updateNsxServiceInstanceAttributesTask.passwordUtil = null;
    }

    public static TaskGraph createServiceOutOfSyncGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(createNsxServiceManagerTask.create(vs));
        expectedGraph.addTask(updateNsxServiceAttributesTask.create(vs));
        expectedGraph.appendTask(nsxDeploymentSpecCheckMetaTask.create(vs, true));
        expectedGraph.appendTask(new RegisterServiceInstanceTask(vs));
        expectedGraph.appendTask(new NsxSecurityGroupInterfacesCheckMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new NsxSecurityGroupsCheckMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    private static VirtualSystem createServiceInstanceOutOfSyncData(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceInstanceId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, null, serviceInstanceId);

        return vs;
    }

    public static TaskGraph createServiceInstanceOutOfSyncGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(createNsxServiceManagerTask.create(vs));
        expectedGraph.appendTask(createNsxServiceTask.create(vs));
        expectedGraph.appendTask(nsxDeploymentSpecCheckMetaTask.create(vs, false));
        expectedGraph.addTask(updateNsxServiceInstanceAttributesTask.create(vs));
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    private static VirtualSystem createVsPolicyMarkedForDeletionData(Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds,
            String policyName,
            Boolean policyDeletion,
            String vendorTemplateId) {
        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId, asvImageUrl, deploymentSpecIds, policyName, policyDeletion, vendorTemplateId);

        return vs;
    }

    public static TaskGraph createVsPolicyMarkedForDeletionGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(createNsxServiceManagerTask.create(vs));
        expectedGraph.appendTask(createNsxServiceTask.create(vs));
        expectedGraph.appendTask(nsxDeploymentSpecCheckMetaTask.create(vs, false));
        expectedGraph.appendTask(new RegisterServiceInstanceTask(vs));
        VirtualSystemPolicy vsp = (VirtualSystemPolicy)vs.getVirtualSystemPolicies().toArray()[0];
        expectedGraph.appendTask(new DeleteDefaultServiceProfileTask(vsp));
        expectedGraph.appendTask(new RemoveVendorTemplateTask(vsp));
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createVsPolicyWithoutTemplateData(Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds,
            String policyName,
            Boolean policyDeletion,
            String vendorTemplateId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId, asvImageUrl, deploymentSpecIds, policyName, policyDeletion, vendorTemplateId);

        return vs;
    }

    public static TaskGraph createVsPolicyWithoutTemplateGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(createNsxServiceManagerTask.create(vs));
        expectedGraph.appendTask(createNsxServiceTask.create(vs));
        expectedGraph.appendTask(nsxDeploymentSpecCheckMetaTask.create(vs, false));
        expectedGraph.appendTask(new RegisterServiceInstanceTask(vs));
        VirtualSystemPolicy vsp = (VirtualSystemPolicy)vs.getVirtualSystemPolicies().toArray()[0];
        expectedGraph.appendTask(new RegisterVendorTemplateTask(vsp));
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    public static VirtualSystem createDomainPolicyOnlyData(Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds,
            String policyName,
            Boolean policyDeletion,
            String vendorTemplateId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId, asvImageUrl, deploymentSpecIds, policyName, policyDeletion, vendorTemplateId);

        return vs;
    }

    public static TaskGraph createDomainPolicyOnlyGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(createNsxServiceManagerTask.create(vs));
        expectedGraph.appendTask(createNsxServiceTask.create(vs));
        expectedGraph.appendTask(nsxDeploymentSpecCheckMetaTask.create(vs, false));
        expectedGraph.appendTask(new RegisterServiceInstanceTask(vs));
        Policy policy = (Policy)vs.getDomain().getPolicies().toArray()[0];
        expectedGraph.appendTask(new RegisterVendorTemplateTask(vs, policy));
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    public static VirtualSystem createVsPolicyWithoutServiceIdData(Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds,
            String policyName,
            Boolean policyDeletion,
            String vendorTemplateId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId, asvImageUrl, deploymentSpecIds, policyName, policyDeletion, vendorTemplateId);

        return vs;
    }


    public static TaskGraph createVsPolicyWithoutServiceIdGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(createNsxServiceManagerTask.create(vs));
        expectedGraph.appendTask(createNsxServiceTask.create(vs));
        expectedGraph.appendTask(nsxDeploymentSpecCheckMetaTask.create(vs, false));
        expectedGraph.appendTask(new RegisterServiceInstanceTask(vs));
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    public static VirtualSystem createVsPolicyNameOutOfSyncData(Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds,
            String policyName,
            Boolean policyDeletion,
            String vendorTemplateId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId, asvImageUrl, deploymentSpecIds, policyName, policyDeletion, vendorTemplateId);

        return vs;
    }

    public static TaskGraph createVsPolicyNameOutOfSyncGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(createNsxServiceManagerTask.create(vs));
        expectedGraph.appendTask(nsxDeploymentSpecCheckMetaTask.create(vs, false));
        expectedGraph.appendTask(new RegisterServiceInstanceTask(vs));
        VirtualSystemPolicy vsp = (VirtualSystemPolicy)vs.getVirtualSystemPolicies().toArray()[0];
        expectedGraph.appendTask(new UpdateVendorTemplateTask(vsp, vsp.getPolicy().getName()));
        expectedGraph.appendTask(new NsxSecurityGroupInterfacesCheckMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new NsxSecurityGroupsCheckMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createOpenstackNoDeploymentSpecData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, null, null);
        vs.getVirtualizationConnector().setVirtualizationType(VirtualizationType.OPENSTACK);
        vs.setKeyStore(new byte[1]);

        return vs;
    }

    public static TaskGraph createOpenstackNoDeploymentSpecGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createOpenstackWithDeploymentSpecData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, null, null);

        DeploymentSpec ds = new DeploymentSpec(vs, null, null, null, null, null);
        ds.setTenantName("OpenstackWithDeploymentSpecTenantName");
        Set<DeploymentSpec> dsSet = new HashSet<DeploymentSpec>();
        dsSet.add(ds);

        vs.setDeploymentSpecs(dsSet);
        vs.getVirtualizationConnector().setVirtualizationType(VirtualizationType.OPENSTACK);

        UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_TASK = createUnlockObjectMetaTask(vs);

        return vs;
    }

    public static TaskGraph createOpenstackWithDeploymentSpecGraph(VirtualSystem vs) throws EncryptionException {
        DeploymentSpec ds = (DeploymentSpec) vs.getDeploymentSpecs().toArray()[0];

        TaskGraph expectedGraph = new TaskGraph();
        Endpoint endPoint = new Endpoint(vs.getVirtualizationConnector(), ds.getTenantName());
        expectedGraph.appendTask(new DSConformanceCheckMetaTask(ds, endPoint));
        expectedGraph.appendTask(UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_TASK, TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createOpenstacWhenLockingDeploymentSpecFailsData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, null, null);

        DeploymentSpec ds = new DeploymentSpec(vs, null, null, null, null, null);
        ds.setTenantName("OpenstacWhenLockingDeploymentSpecFails");
        Set<DeploymentSpec> dsSet = new HashSet<DeploymentSpec>();
        dsSet.add(ds);

        vs.setDeploymentSpecs(dsSet);
        vs.getVirtualizationConnector().setVirtualizationType(VirtualizationType.OPENSTACK);

        return vs;
    }

    public static TaskGraph createOpenstacWhenLockingDeploymentSpecFailsGraph(VirtualSystem vs) {
        DeploymentSpec ds = (DeploymentSpec) vs.getDeploymentSpecs().toArray()[0];

        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new FailedWithObjectInfoTask("Acquiring Write lock for Deployment Specification", "null",
                LockObjectReference.getObjectReferences(ds)), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    private static VirtualSystem createDeleteServiceInstanceData(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceInstanceId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, null, serviceInstanceId);
        vs.setMarkedForDeletion(true);

        return vs;
    }

    public static TaskGraph createDeleteServiceInstanceGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new DeleteServiceInstanceTask(vs));
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs));
        expectedGraph.appendTask(new SecurityGroupCleanupCheckMetaTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new MgrDeleteVSSDeviceTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new DeleteVsFromDbTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createDeleteServiceManagerData(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, serviceManagerId, null, null);
        vs.setMarkedForDeletion(true);

        return vs;
    }

    public static TaskGraph createDeleteServiceManagerGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new UnregisterServiceManagerCallbackTask(vs));
        expectedGraph.appendTask(new DeleteServiceManagerTask(vs));
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs));
        expectedGraph.appendTask(new SecurityGroupCleanupCheckMetaTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new MgrDeleteVSSDeviceTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new DeleteVsFromDbTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createUndenployServiceInstanceData(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceId,
            String serviceInstanceId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, serviceId, serviceInstanceId);
        vs.setMarkedForDeletion(true);

        return vs;
    }

    public static TaskGraph createUndenployServiceInstanceGraph(VirtualSystem vs) {
        Service service = new Service();
        service.setId(vs.getNsxServiceInstanceId());

        ServiceProfile sp = new ServiceProfile();
        sp.setService(service);
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new DeleteServiceInstanceTask(vs));
        expectedGraph.appendTask(new NsxSecurityGroupInterfacesCheckMetaTask(vs));
        expectedGraph.appendTask(new DeleteServiceTask(vs));
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs));
        expectedGraph.appendTask(new SecurityGroupCleanupCheckMetaTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new MgrDeleteVSSDeviceTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new DeleteVsFromDbTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    private static VirtualSystem createDeleteServiceData(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, serviceId, null);
        vs.setMarkedForDeletion(true);

        return vs;
    }

    public static TaskGraph createDeleteServiceGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new NsxSecurityGroupInterfacesCheckMetaTask(vs));
        expectedGraph.appendTask(new DeleteServiceTask(vs));
        expectedGraph.appendTask(new ValidateNsxAgentsTask(vs));
        expectedGraph.appendTask(new SecurityGroupCleanupCheckMetaTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new MgrDeleteVSSDeviceTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new DeleteVsFromDbTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createDeleteOpenStackWithDeploymentSpecData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, null, null);

        DeploymentSpec ds = new DeploymentSpec(vs, null, null, null, null, null);
        ds.setTenantName("DeleteOpenStackWithDeploymentSpecTenantName");
        Set<DeploymentSpec> dsSet = new HashSet<DeploymentSpec>();
        dsSet.add(ds);

        vs.setDeploymentSpecs(dsSet);
        vs.getVirtualizationConnector().setVirtualizationType(VirtualizationType.OPENSTACK);
        vs.setMarkedForDeletion(true);


        return vs;
    }

    public static TaskGraph createDeleteOpenStackWithDeploymentSpecGraph(VirtualSystem vs) throws EncryptionException {
        DeploymentSpec ds = (DeploymentSpec) vs.getDeploymentSpecs().toArray()[0];

        TaskGraph expectedGraph = new TaskGraph();
        Endpoint endPoint = new Endpoint(vs.getVirtualizationConnector(), ds.getTenantName());
        expectedGraph.appendTask(new DSConformanceCheckMetaTask(ds, endPoint));
        expectedGraph.appendTask(new SecurityGroupCleanupCheckMetaTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new MgrDeleteVSSDeviceTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new DeleteVsFromDbTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createDeleteOpenStackWithOSImageRefData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, null, null);
        OsImageReference image = new OsImageReference(vs, "deleteOpenStackWithOSImageRefRegion", "deleteOpenStackWithOSImageRefRefId");
        vs.addOsImageReference(image);
        vs.getVirtualizationConnector().setVirtualizationType(VirtualizationType.OPENSTACK);
        vs.setMarkedForDeletion(true);

        return vs;
    }

    public static TaskGraph createDeleteOpenStackWithOSImageRefGraph(VirtualSystem vs) throws EncryptionException {
        OsImageReference image = (OsImageReference) vs.getOsImageReference().toArray()[0];

        TaskGraph expectedGraph = new TaskGraph();
        Endpoint endPoint = new Endpoint(vs.getVirtualizationConnector(), vs.getVirtualizationConnector().getProviderAdminTenantName());
        expectedGraph.appendTask(new DeleteImageFromGlanceTask(image.getRegion(), image, endPoint));
        expectedGraph.appendTask(new SecurityGroupCleanupCheckMetaTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new MgrDeleteVSSDeviceTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new DeleteVsFromDbTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createDeleteOpenStackWithOSFlavorRefData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, null, null);
        OsFlavorReference flavor = new OsFlavorReference(vs, "deleteOpenStackWithOSFlavorRefRegion", "deleteOpenStackWithOSFlavorRefRefId");
        vs.addOsFlavorReference(flavor);
        vs.getVirtualizationConnector().setVirtualizationType(VirtualizationType.OPENSTACK);
        vs.setMarkedForDeletion(true);

        return vs;
    }

    public static TaskGraph createDeleteOpenStackWithOSFlavorRefGraph(VirtualSystem vs) throws EncryptionException {
        OsFlavorReference flavor = (OsFlavorReference) vs.getOsFlavorReference().toArray()[0];

        TaskGraph expectedGraph = new TaskGraph();
        Endpoint endPoint = new Endpoint(vs.getVirtualizationConnector(), vs.getVirtualizationConnector().getProviderAdminTenantName());
        expectedGraph.appendTask(new DeleteFlavorTask(flavor.getRegion(), flavor, endPoint));
        expectedGraph.appendTask(new SecurityGroupCleanupCheckMetaTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new MgrDeleteVSSDeviceTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new DeleteVsFromDbTask(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static UnlockObjectMetaTask createUnlockObjectMetaTask(VirtualSystem vs) {
        UnlockObjectTask unlockTask = new UnlockObjectTask(new LockObjectReference(vs), LockType.READ_LOCK);
        UnlockObjectMetaTask metaTask = new UnlockObjectMetaTask(Arrays.asList(unlockTask));
        return metaTask;
    }
}
