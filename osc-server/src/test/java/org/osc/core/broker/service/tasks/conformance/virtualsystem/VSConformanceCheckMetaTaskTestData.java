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
import java.util.Set;

import org.mockito.Mockito;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.tasks.FailedWithObjectInfoTask;
import org.osc.core.broker.service.tasks.conformance.GenerateVSSKeysTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteVsFromDbTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrDeleteVSSDeviceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteFlavorTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteImageFromGlanceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DSConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.SecurityGroupCleanupCheckMetaTask;

public class VSConformanceCheckMetaTaskTestData {

    public static String DEFAULT_SERVICE_NAME = "DEFAULT_SERVICE_NAME";

    public static List<VirtualSystem> TEST_VIRTUAL_SYSTEMS;

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

    private static VirtualSystem createVirtualSystem(Long vsId, Long vcId, Long daId) {
        // Mock SslContext
        VirtualizationConnector vcSpy = Mockito.spy(VirtualizationConnector.class);
        vcSpy.setId(vcId);

        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setManagerType(ManagerType.NSM.getValue());

        DistributedAppliance da = new DistributedAppliance(mc);
        da.setId(daId);
        da.setName(DEFAULT_SERVICE_NAME);

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion();

        VirtualSystem vs = new VirtualSystem(da);
        vs.setId(vsId);
        vs.setVirtualizationConnector(vcSpy);
        vs.setDomain(new Domain());
        vs.setApplianceSoftwareVersion(asv);
        vs.setDomain(new Domain());

        if (TEST_VIRTUAL_SYSTEMS == null) {
            TEST_VIRTUAL_SYSTEMS = new ArrayList<VirtualSystem>();
        }

        TEST_VIRTUAL_SYSTEMS.add(vs);
        return vs;
    }

    static ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);

    private static VirtualSystem createOpenstackNoDeploymentSpecData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId);
        vs.getVirtualizationConnector().setVirtualizationType(VirtualizationType.OPENSTACK);
        vs.setKeyStore(new byte[1]);

        return vs;
    }

    public static TaskGraph createOpenstackNoDeploymentSpecGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask().create(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createOpenstackWithDeploymentSpecData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId);

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
        expectedGraph.appendTask(new DSConformanceCheckMetaTask().create(ds, endPoint));
        expectedGraph.appendTask(UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_TASK, TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new GenerateVSSKeysTask().create(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask().create(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createOpenstacWhenLockingDeploymentSpecFailsData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId);

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
        expectedGraph.appendTask(new GenerateVSSKeysTask().create(vs));
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask().create(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    private static VirtualSystem createDeleteOpenStackWithDeploymentSpecData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId);

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
        expectedGraph.appendTask(new DSConformanceCheckMetaTask().create(ds, endPoint));
        expectedGraph.appendTask(new SecurityGroupCleanupCheckMetaTask().create(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new MgrDeleteVSSDeviceTask().create(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new DeleteVsFromDbTask().create(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createDeleteOpenStackWithOSImageRefData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId);
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
        expectedGraph.appendTask(new DeleteImageFromGlanceTask().create(image.getRegion(), image, endPoint));
        expectedGraph.appendTask(new SecurityGroupCleanupCheckMetaTask().create(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new MgrDeleteVSSDeviceTask().create(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new DeleteVsFromDbTask().create(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static VirtualSystem createDeleteOpenStackWithOSFlavorRefData(
            Long vsId,
            Long vcId,
            Long daId) {

        VirtualSystem vs = createVirtualSystem(vsId, vcId, daId);
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
        expectedGraph.appendTask(new DeleteFlavorTask().create(flavor.getRegion(), flavor, endPoint));
        expectedGraph.appendTask(new SecurityGroupCleanupCheckMetaTask().create(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new MgrDeleteVSSDeviceTask().create(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new DeleteVsFromDbTask().create(vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(vs.getVirtualizationConnector()), LockType.READ_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    private static UnlockObjectMetaTask createUnlockObjectMetaTask(VirtualSystem vs) {
        UnlockObjectTask unlockTask = new UnlockObjectTask(new LockObjectReference(vs), LockType.READ_LOCK);
        UnlockObjectMetaTask metaTask = new UnlockObjectMetaTask(Arrays.asList(unlockTask));
        return metaTask;
    }
}