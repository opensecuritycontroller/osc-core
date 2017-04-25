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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.osc.core.broker.job.TaskGraph;
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
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.rest.client.nsx.model.VersionedDeploymentSpec;
import org.osc.core.broker.service.tasks.network.UpdateNsxDeploymentSpecTask;
import org.osc.core.broker.service.tasks.network.UpdateNsxServiceInstanceAttributesTask;
import org.osc.core.broker.service.tasks.passwordchange.UpdateNsxServiceAttributesTask;

public class NsxDeploymentSpecCheckMetaTaskTestData {

    public static String DEFAULT_SERVICE_NAME = "DEFAULT_SERVICE_NAME";
    public static String IMAGE_URL_OUT_OF_SYNC = "imageUrlOutOfSync";

    public static List<VirtualSystem> TEST_VIRTUAL_SYSTEMS;

    public static VirtualSystem VMWARE_NEW_DIST_APPL_NO_DEPLOYMENT_SPEC_VS =
            createVmwareNoDeploymentSpecData(
                    1L,
                    100L,
                    101L,
                    "VMWARE_NEW_DIST_APPL_NO_DEPLOYMENT_SPEC_SM_ID",
                    "VMWARE_NEW_DIST_APPL_NO_DEPLOYMENT_SPEC_S_ID",
                    null,
                    null,
                    null
                    );

    public static VirtualSystem VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_VS =
            createApplianceVersionImageUrlOutOfSyncData(
                    2L,
                    200L,
                    201L,
                    "VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_SM_ID",
                    "VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_S_ID",
                    null,
                    "VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC",
                    null
            );

    public static VirtualSystem VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_UPDATE_NSX_SCHED_VS =
            createApplianceVersionImageUrlOutOfSyncData(
                    3L,
                    300L,
                    301L,
                    "DEPL_SPEC_IMAGE_URL_OUT_OF_SYNC_UPDATE_NSX_SCHED_SM_ID",
                    "DEPL_SPEC_IMAGE_URL_OUT_OF_SYNC_UPDATE_NSX_SCHED_S_ID",
                    null,
                    "DEPL_SPEC_IMAGE_URL_OUT_OF_SYNC_UPDATE_NSX_SCHED",
                    null
            );

    public static VirtualSystem VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_VS =
            createDBUpgradeData(
                    4L,
                    400L,
                    401L,
                    "VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_SM_ID",
                    "VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_S_ID",
                    null,
                    null,
                    null
            );

    public static VirtualSystem VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_NSX_OOS_VS =
            createDBUpgradeData(
                    5L,
                    500L,
                    501L,
                    "VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_NSX_OOS_SM_ID",
                    "VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_NSX_OOS_S_ID",
                    null,
                    null,
                    null
            );

    public static VirtualSystem VMWARE_NSX_DS_OUT_OF_SYNC_6_X_VS =
            createNsxDeploymentSpecOutOfSyncData(
                    6L,
                    600L,
                    601L,
                    "VMWARE_NSX_DS_OUT_OF_SYNC_6X_SM_ID",
                    "VMWARE_NSX_DS_OUT_OF_SYNC_6X_S_ID",
                    null,
                    "DEPLOYMENTSPEC_IMAGE_URL_OUT_OF_SYNC_6X",
                    null
            );

    public static VirtualSystem VMWARE_NSX_DS_OUT_OF_SYNC_5_5_X_VS =
            createNsxDeploymentSpecOutOfSyncData(
                    7L,
                    700L,
                    701L,
                    "VMWARE_NSX_DS_OUT_OF_SYNC_5_5_X_SM_ID",
                    "VMWARE_NSX_DS_OUT_OF_SYNC_5_5_X_S_ID",
                    null,
                    "DEPLOYMENTSPEC_IMAGE_URL_OUT_OF_SYNC_5_5_X_",
                    null
            );
    public static VirtualSystem VMWARE_NSX_ALL_DEPLOY_SPEC_MISSING_VS =
            createNsxDeploymentSpecOutOfSyncData(
                    8L,
                    800L,
                    801L,
                    "VMWARE_NSX_DS_OUT_OF_SYNC_ALL_SM_ID",
                    "VMWARE_NSX_DS_OUT_OF_SYNC_ALL_S_ID",
                    null,
                    "DEPLOYMENTSPEC_IMAGE_URL_OUT_OF_SYNC_ALL",
                    null
            );


    private static UpdateNsxServiceAttributesTask updateNsxServiceAttributesTask = new UpdateNsxServiceAttributesTask();
    private static UpdateNsxServiceInstanceAttributesTask updateNsxServiceInstanceAttributesTask = new UpdateNsxServiceInstanceAttributesTask();

    public static VirtualSystem createVmwareNoDeploymentSpecData(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, null, null, null);
        vs.getVirtualizationConnector().setVirtualizationType(VirtualizationType.VMWARE);
        vs.setKeyStore(new byte[1]);

        return vs;
    }

    @SuppressWarnings("serial")
    private static VirtualSystem createApplianceVersionImageUrlOutOfSyncData(
            Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId,
                asvImageUrl, deploymentSpecIds);

        deploymentSpecIds = new HashMap<VmwareSoftwareVersion, String>(){{
            put(VmwareSoftwareVersion.VMWARE_V5_5, null);
            put(VmwareSoftwareVersion.VMWARE_V6,  null);
        }};
        vs.setNsxDeploymentSpecIds(deploymentSpecIds);

        return vs;
    }

    @SuppressWarnings("serial")
    private static VirtualSystem createNsxDeploymentSpecOutOfSyncData(Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId,
                asvImageUrl, deploymentSpecIds);

        deploymentSpecIds = new HashMap<VmwareSoftwareVersion, String>(){{
            put(VmwareSoftwareVersion.VMWARE_V5_5, null);
            put(VmwareSoftwareVersion.VMWARE_V6,  null);
        }};
        vs.setNsxDeploymentSpecIds(deploymentSpecIds);

        return vs;
    }

    @SuppressWarnings("serial")
    private static VirtualSystem createDBUpgradeData(Long vsId,
            Long vcId,
            Long daId,
            String serviceManagerId,
            String serviceId,
            String serviceInstanceId,
            String asvImageUrl,
            Map<VmwareSoftwareVersion, String> deploymentSpecIds) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId,
                asvImageUrl, deploymentSpecIds);

        deploymentSpecIds = new HashMap<VmwareSoftwareVersion, String>(){{
            put(VmwareSoftwareVersion.VMWARE_V5_5, null);
        }};
        vs.setNsxDeploymentSpecIds(deploymentSpecIds);

        return vs;
    }

    private static VirtualSystem createVirtualSystem(Long vsId, Long vcId, Long daId, String serviceManagerId,
            String serviceId, String serviceInstanceId) {
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
        return createVirtualSystem(vsId, vcId, daId, serviceManagerId, serviceId, serviceInstanceId, asvImageUrl,
                deploymentSpecIds, null, null, null);
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
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setVirtualizationType(VirtualizationType.VMWARE);
        vc.setId(vcId);

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
        vs.setVirtualizationConnector(vc);
        vs.setDomain(new Domain());
        vs.setNsxServiceInstanceId(serviceInstanceId);
        if (!MapUtils.isEmpty(deploymentSpecIds)){
            vs.setNsxDeploymentSpecIds(deploymentSpecIds);
        }

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

    public static TaskGraph createApplianceVersionImageUrlOutOfSyncGraph(VirtualSystem vs) {
        Map<VmwareSoftwareVersion, String> vs_ds = vs.getNsxDeploymentSpecIds();
        TaskGraph expectedGraph = new TaskGraph();
        for (VmwareSoftwareVersion sw : vs_ds.keySet()){
            VersionedDeploymentSpec spec = new VersionedDeploymentSpec();
            spec.setHostVersion(org.osc.core.broker.model.virtualization.VmwareSoftwareVersion.valueOf(sw.name())
                    .toString() + RegisterDeploymentSpecTask.ALL_MINOR_VERSIONS);
            spec.setOvfUrl(IMAGE_URL_OUT_OF_SYNC);
            expectedGraph.addTask(new UpdateNsxDeploymentSpecTask(vs, spec));
        }
        expectedGraph.addTask(updateNsxServiceAttributesTask.create(vs));
        expectedGraph.addTask(updateNsxServiceInstanceAttributesTask.create(vs));
        return expectedGraph;
    }

    public static TaskGraph createApplVersionImageUrlOutOfSyncUpdateNsxSchedGraph(VirtualSystem vs) {
        Map<VmwareSoftwareVersion, String> vs_ds = vs.getNsxDeploymentSpecIds();
        TaskGraph expectedGraph = new TaskGraph();
        for (VmwareSoftwareVersion sw : vs_ds.keySet()){
            VersionedDeploymentSpec spec = new VersionedDeploymentSpec();
            spec.setHostVersion(org.osc.core.broker.model.virtualization.VmwareSoftwareVersion.valueOf(sw.name())
                    .toString() + RegisterDeploymentSpecTask.ALL_MINOR_VERSIONS);
            spec.setOvfUrl(IMAGE_URL_OUT_OF_SYNC);
            expectedGraph.addTask(new UpdateNsxDeploymentSpecTask(vs, spec));
        }
        expectedGraph.addTask(updateNsxServiceInstanceAttributesTask.create(vs));
        return expectedGraph;
    }

    public static TaskGraph createRegisterDeploySpecExpectedGraph(VirtualSystem vs, VmwareSoftwareVersion... softwareVersions) {
        TaskGraph expectedGraph = new TaskGraph();
        List<VmwareSoftwareVersion> versions = Arrays.asList(softwareVersions);
        for (VmwareSoftwareVersion version : versions) {
            expectedGraph.appendTask(new RegisterDeploymentSpecTask(vs , version));
        }
        return expectedGraph;
    }

}
