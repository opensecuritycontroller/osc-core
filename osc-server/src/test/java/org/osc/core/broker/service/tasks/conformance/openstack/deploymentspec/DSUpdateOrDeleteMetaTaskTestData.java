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
package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.service.tasks.FailedWithObjectInfoTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteOsSecurityGroupTask;
import org.osc.core.common.virtualization.VirtualizationType;

public class DSUpdateOrDeleteMetaTaskTestData {
    public static List<DeploymentSpec> TEST_DEPLOYMENT_SPECS = new ArrayList<>();
    public static String AZ_1 = "AZ_1";
    public static String HS_1_1 = "HS_1_1";
    public static String REGION_1 = "REGION_1";

    public static List<DistributedApplianceInstance> UPDATE_AZ_SELECTED_DAIS =
            new ArrayList<>();
    public static List<DistributedApplianceInstance> UPDATE_DAI_HOST_NOT_IN_AZ_DAIS =
            new ArrayList<>();
    public static List<DistributedApplianceInstance> UPDATE_OPENSTACK_AZ_NOT_SELECTED_DAIS =
            new ArrayList<>();
    public static List<DistributedApplianceInstance> UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_DAIS =
            new ArrayList<>();

    public static DeploymentSpec UPDATE_NO_HOST_SELECTED_DS =
            createAllHostsInRegionData(
                    "UPDATE_NO_HOST_SELECTED_DS",
                    REGION_1);

    public static DeploymentSpec UPDATE_DAI_HOST_SELECTED_DS =
            createDsWithDaiAndHostSelectedData(
                    "UPDATE_DAI_HOST_SELECTED_DS",
                    REGION_1,
                    HS_1_1,
                    HS_1_1,
                    "UPDATE_CONFORM_DAI_DAINAME");

    public static DeploymentSpec UPDATE_DAI_HOST_NOT_SELECTED_DS =
            createDsWithDaiAndHostSelectedData(
                    "UPDATE_DAI_HOST_NOT_SELECTED_DS",
                    REGION_1,
                    "UPDATE_DAI_HOST_NOT_SELECTED_DAI_HOSTNAME",
                    HS_1_1,
                    "UPDATE_DAI_HOST_NOT_SELECTED_DAINAME");

    public static DeploymentSpec UPDATE_MULT_DAI_ONE_HOST_SELECTED_DS =
            createDsWithMultipleDaiAndHostSelectedData(
                    "UPDATE_MULT_DAI_ONE_HOST_SELECTED_DS",
                    REGION_1,
                    HS_1_1,
                    "UPDATE_DAI_HOST_NOT_SELECTED_DAI_HOSTNAME",
                    HS_1_1,
                    "UPDATE_MULT_DAI_ONE_HOST_SELECTED_DAINAME");

    public static DeploymentSpec UPDATE_AZ_SELECTED_DS =
            createDsWithAvailabilityZoneSelectedData(
                    "UPDATE_AZ_SELECTED_DS",
                    REGION_1,
                    HS_1_1,
                    "UPDATE_AZ_SELECTED_DAINAME",
                    AZ_1,
                    UPDATE_AZ_SELECTED_DAIS);

    public static DeploymentSpec UPDATE_DAI_HOST_NOT_IN_AZ_DS =
            createDsWithAvailabilityZoneSelectedData(
                    "UPDATE_DAI_HOST_NOT_IN_AZ_DS",
                    REGION_1,
                    "UPDATE_DAI_NOT_IN_AZ_HOSTNAME",
                    "UPDATE_DAI_NOT_IN_AZ_DAINAME",
                    AZ_1,
                    UPDATE_DAI_HOST_NOT_IN_AZ_DAIS);

    public static DeploymentSpec UPDATE_OPENSTACK_AZ_NOT_SELECTED_DS =
            createDsWithAvailabilityZoneSelectedData(
                    "UPDATE_OPENSTACK_AZ_NOT_SELECTED_DS",
                    REGION_1,
                    "UPDATE_DAI_NOT_IN_AZ_HOSTNAME",
                    "UPDATE_DAI_NOT_IN_AZ_DAINAME",
                    "UPDATE_OS_AZ_NOT_SELECTED",
                    UPDATE_OPENSTACK_AZ_NOT_SELECTED_DAIS);

    public static DeploymentSpec UPDATE_HOST_AGGREGATE_SELECTED_DS =
            createDsWithHostAggregateSelectedData(
                    "UPDATE_HOST_AGGREGATE_SELECTED_DS",
                    REGION_1,
                    "UPDATE_HOST_AGGREGATE_SELECTED_HA_ID");

    public static DeploymentSpec UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_DS =
            createDsWithDaiAndHostAggregateSelectedData(
                    "UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_DS",
                    REGION_1,
                    "UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_HOSTNAME",
                    "UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_DAINAME",
                    "UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_HA_ID",
                    UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_DAIS);

    public static DeploymentSpec DELETE_DS_WITHOUT_SG_REFERENCE =
            createDsForDeletionWithoutSgReferenceData(
                    "DELETE_DS_WITHOUT_SG_REFERENCE",
                    "DELETE_DAINAME");

    public static DeploymentSpec DELETE_DS_WITH_SG_REFERENCE =
            createDsForDeletionWithSgReferenceData(
                    "DELETE_DS_WITH_SG_REFERENCE",
                    "DELETE_DAINAME");

    private static DeploymentSpec createAllHostsInRegionData(String dsName, String region) {
        return createDeploymentSpec(1L, dsName, region);
    }

    private static DeploymentSpec createDsWithDaiAndHostSelectedData(String dsName, String region, String daiHostName, String selectedHostName, String daiName) {
        DeploymentSpec ds = createDeploymentSpec(1L, dsName, region);
        DistributedApplianceInstance dai = new DistributedApplianceInstance(ds.getVirtualSystem());
        dai.setDeploymentSpec(ds);
        dai.setOsHostName(daiHostName);
        dai.setName(daiName);

        ds.setDistributedApplianceInstances(new HashSet<>(Arrays.asList(dai)));
        ds.getVirtualSystem().addDistributedApplianceInstance(dai);

        Host host = new Host(ds, dsName + "_openstackId");
        host.setName(selectedHostName);
        ds.setHosts(new HashSet<>(Arrays.asList(host)));

        return ds;
    }

    private static DeploymentSpec createDsWithMultipleDaiAndHostSelectedData(String dsName, String region, String daiHostName1, String daiHostName2, String selectedHostName, String daiName) {
        DeploymentSpec ds = createDsWithDaiAndHostSelectedData(dsName, region, daiHostName1, selectedHostName, daiName + "_1");

        DistributedApplianceInstance dai = new DistributedApplianceInstance(ds.getVirtualSystem());
        dai.setDeploymentSpec(ds);
        dai.setOsHostName(daiHostName2);
        dai.setName(daiName + "_2");

        ds.getDistributedApplianceInstances().add(dai);
        ds.getVirtualSystem().addDistributedApplianceInstance(dai);
        return ds;
    }

    private static DeploymentSpec createDsForDeletionWithoutSgReferenceData(String dsName, String daiName) {
        DeploymentSpec ds = createDsWithDaiAndHostSelectedData(dsName, REGION_1, "foo", "bar", daiName);
        ds.setMarkedForDeletion(true);

        return ds;
    }

    private static DeploymentSpec createDsForDeletionWithSgReferenceData(String dsName, String daiName) {
        DeploymentSpec ds = createDsWithDaiAndHostSelectedData(dsName, REGION_1, "foo", "bar", daiName);
        ds.setOsSecurityGroupReference(new OsSecurityGroupReference(dsName + "_sgRefId",
                dsName + "_sgRefName", ds));
        ds.setMarkedForDeletion(true);

        return ds;
    }

    private static DeploymentSpec createDsWithAvailabilityZoneSelectedData(String dsName, String region, String daiHostName, String daiName, String selectedAzName, List<DistributedApplianceInstance> dais) {
        DeploymentSpec ds = createDeploymentSpec(1L, dsName, region);

        AvailabilityZone az = new AvailabilityZone(ds, region, selectedAzName);
        ds.setAvailabilityZones(new HashSet<>(Arrays.asList(az)));

        DistributedApplianceInstance dai = new DistributedApplianceInstance(ds.getVirtualSystem());
        dai.setDeploymentSpec(ds);
        dai.setOsHostName(daiHostName);
        dai.setOsAvailabilityZone(selectedAzName);
        dai.setName(daiName);

        ds.setDistributedApplianceInstances(new HashSet<>(Arrays.asList((dai))));
        ds.getVirtualSystem().addDistributedApplianceInstance(dai);

        dais.add(dai);
        return ds;
    }

    private static DeploymentSpec createDsWithHostAggregateSelectedData(String dsName, String region, String hostAggregateOSId) {
        DeploymentSpec ds = createDeploymentSpec(1L, dsName, region);

        HostAggregate ha = new HostAggregate(ds, hostAggregateOSId);
        ha.setName(dsName + "_ha");

        ds.setHostAggregates(new HashSet<>(Arrays.asList(ha)));
        return ds;
    }

    private static DeploymentSpec createDsWithDaiAndHostAggregateSelectedData(String dsName, String region, String daiHostName, String daiName, String hostAggregateOSId, List<DistributedApplianceInstance> dais) {
        DeploymentSpec ds = createDsWithHostAggregateSelectedData(dsName, region, hostAggregateOSId);
        DistributedApplianceInstance dai = new DistributedApplianceInstance(ds.getVirtualSystem());
        dai.setDeploymentSpec(ds);
        dai.setOsHostName(daiHostName);
        dai.setName(daiName);

        dais.add(dai);
        ds.setDistributedApplianceInstances(new HashSet<>(Arrays.asList(dai)));
        ds.getVirtualSystem().addDistributedApplianceInstance(dai);

        return ds;
    }

    public static TaskGraph createAllHostsInRegionGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new OsSvaCreateMetaTask().create(ds, HS_1_1, AZ_1));
        return expectedGraph;
    }

    public static TaskGraph createDAIHostSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(
                new OsDAIConformanceCheckMetaTask().create(
                        ds.getDistributedApplianceInstances().iterator().next(),
                        true));
        return expectedGraph;
    }

    public static TaskGraph createDAIHostNotSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        Task firstCreateTask = new OsSvaCreateMetaTask().create(ds, HS_1_1, AZ_1);
        expectedGraph.addTask(firstCreateTask);
        expectedGraph.addTask(new DeleteSvaServerAndDAIMetaTask().create(ds.getRegion(),
                ds.getDistributedApplianceInstances().iterator().next()),
                firstCreateTask);

        return expectedGraph;
    }

    public static TaskGraph createMultippleDAIOneHostSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();

        DistributedApplianceInstance daiToConform = ds.getDistributedApplianceInstances().stream()
                .filter(d -> d.getOsHostName().equals(HS_1_1)).findFirst().get();

        DistributedApplianceInstance daiToDelete = ds.getDistributedApplianceInstances().stream()
                .filter(d -> !d.getOsHostName().equals(HS_1_1)).findFirst().get();

        Task firstAddTask = new OsDAIConformanceCheckMetaTask().create(daiToConform, true);
        expectedGraph.addTask(firstAddTask);
        expectedGraph.addTask(new DeleteSvaServerAndDAIMetaTask().create(ds.getRegion(), daiToDelete), firstAddTask);

        return expectedGraph;
    }

    public static TaskGraph createDAIHostAggregateNotSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
		expectedGraph.addTask(new FailedWithObjectInfoTask(
				String.format("Create SVA for Host Aggregate %s(%s) in Region '%s'", ds.getHostAggregates().iterator()
						.next().getName(), ds.getHostAggregates().iterator().next().getId(), ds.getRegion()),
				String.format("Host Aggregate %s(%s) has been deleted from openstack or invalid. Deleting from DS.",
						ds.getHostAggregates().iterator().next().getName(),
						ds.getHostAggregates().iterator().next().getId()),
				LockObjectReference.getObjectReferences(ds)));
        expectedGraph.addTask(new DeleteSvaServerAndDAIMetaTask().create(ds.getRegion(),
                ds.getDistributedApplianceInstances().iterator().next()));
        return expectedGraph;
    }

    public static TaskGraph createAZSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new OsDAIConformanceCheckMetaTask().create(UPDATE_AZ_SELECTED_DAIS.iterator().next(), true));
        return expectedGraph;
    }

    public static TaskGraph createDaiHostNotInAZSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        Task conformCheck = new OsDAIConformanceCheckMetaTask().create(UPDATE_DAI_HOST_NOT_IN_AZ_DAIS.iterator().next(), false);

        expectedGraph.addTask(conformCheck);
        expectedGraph.addTask(new OsSvaCreateMetaTask().create(ds, HS_1_1, (ds.getAvailabilityZones().iterator().next()).getZone()), conformCheck);

        return expectedGraph;
    }

    public static TaskGraph createOpenStackAZNotSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        Task firstCreateTask = new OsDAIConformanceCheckMetaTask().create(UPDATE_OPENSTACK_AZ_NOT_SELECTED_DAIS.iterator().next(), false);
        expectedGraph.addTask(firstCreateTask);
		expectedGraph.addTask(new FailedWithObjectInfoTask(
				String.format("Create SVA for Availability Zone '%s' in Region '%s'",
						ds.getAvailabilityZones().iterator().next().getZone(), ds.getRegion()),
				String.format("Availability Zone '%s' is not available",
						ds.getAvailabilityZones().iterator().next().getZone()),
				LockObjectReference.getObjectReferences(ds)));
        expectedGraph.addTask(new DeleteSvaServerAndDAIMetaTask().create(ds.getRegion(), UPDATE_OPENSTACK_AZ_NOT_SELECTED_DAIS.iterator().next()),
                firstCreateTask);
        return expectedGraph;
    }

    public static TaskGraph createDeleteDsGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new DeleteSvaServerAndDAIMetaTask().create(ds.getRegion(), ds.getDistributedApplianceInstances().iterator().next()));
        if (ds.getOsSecurityGroupReference() != null) {
            expectedGraph.appendTask(new DeleteOsSecurityGroupTask().create(ds, ds.getOsSecurityGroupReference()));
        }
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask().create(ds.getVirtualSystem()));
        expectedGraph.appendTask(new DeleteDSFromDbTask().create(ds));
        return expectedGraph;
    }

    private static DeploymentSpec createDeploymentSpec(Long dsId, String baseName, String region) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName(baseName + "_vc");
        vc.setVirtualizationType(VirtualizationType.OPENSTACK);
        vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        vc.setProviderIpAddress(baseName + "_providerIp");
        vc.setProviderUsername("Natasha");
        vc.setProviderPassword("********");

        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setIpAddress(baseName + "_mcIp");
        mc.setName(baseName + "_mc");
        mc.setServiceType("foobar");
        mc.setManagerType("buzz");

        Domain domain = new Domain(mc);
        domain.setName(baseName + "_domain");

        Appliance app = new Appliance();
        app.setManagerSoftwareVersion("fizz");
        app.setManagerType("buzz");
        app.setModel(baseName + "_model");

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion(app);
        asv.setApplianceSoftwareVersion("softwareVersion");
        asv.setImageUrl(baseName + "_image");
        asv.setVirtualizarionSoftwareVersion(vc.getVirtualizationSoftwareVersion());
        asv.setVirtualizationType(vc.getVirtualizationType());

        DistributedAppliance da = new DistributedAppliance(mc);
        da.setName(baseName + "_da");
        da.setApplianceVersion("foo");
        da.setAppliance(app);

        VirtualSystem vs = new VirtualSystem(da);
        vs.setApplianceSoftwareVersion(asv);
        vs.setDomain(domain);
        vs.setVirtualizationConnector(vc);
        vs.setMarkedForDeletion(false);
        vs.setName(baseName + "_vs");
        vs.setMgrId(baseName + "_mgrId");

        DeploymentSpec ds = new DeploymentSpec(vs, region, baseName + "_projectId",
                baseName + "_mnId",baseName + "_inId", null);
        ds.setId(dsId);
        ds.setName(baseName + "_ds");
        ds.setProjectName(baseName + "_projectName");
        ds.setManagementNetworkName(baseName + "_mnName");
        ds.setInspectionNetworkName(baseName + "_inName");

        TEST_DEPLOYMENT_SPECS.add(ds);
        return ds;
    }
}
