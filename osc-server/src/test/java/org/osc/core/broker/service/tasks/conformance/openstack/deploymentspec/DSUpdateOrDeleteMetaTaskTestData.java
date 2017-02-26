package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteOsSecurityGroupTask;

public class DSUpdateOrDeleteMetaTaskTestData {
    public static List<DeploymentSpec> TEST_DEPLOYMENT_SPECS = new ArrayList<>();
    public static String AZ_1 = "AZ_1";
    public static String HS_1_1 = "HS_1_1";
    public static String REGION_1 = "REGION_1";

    private static VirtualSystem VIRTUALSYSTEM = new VirtualSystem();

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
                    1L,
                    REGION_1);

    public static DeploymentSpec UPDATE_DAI_HOST_SELECTED_DS =
            createDsWithDaiAndHostSelectedData(
                    2L,
                    REGION_1,
                    HS_1_1,
                    HS_1_1,
                    "UPDATE_CONFORM_DAI_DAINAME");

    public static DeploymentSpec UPDATE_DAI_HOST_NOT_SELECTED_DS =
            createDsWithDaiAndHostSelectedData(
                    3L,
                    REGION_1,
                    "UPDATE_DAI_HOST_NOT_SELECTED_DAI_HOSTNAME",
                    HS_1_1,
                    "UPDATE_DAI_HOST_NOT_SELECTED_DAINAME");

    public static DeploymentSpec UPDATE_AZ_SELECTED_DS =
            createDsWithAvailabilityZoneSelectedData(
                    4L,
                    REGION_1,
                    HS_1_1,
                    "UPDATE_AZ_SELECTED_DAINAME",
                    AZ_1,
                    UPDATE_AZ_SELECTED_DAIS);

    public static DeploymentSpec UPDATE_DAI_HOST_NOT_IN_AZ_DS =
            createDsWithAvailabilityZoneSelectedData(
                    5L,
                    REGION_1,
                    "UPDATE_DAI_NOT_IN_AZ_HOSTNAME",
                    "UPDATE_DAI_NOT_IN_AZ_DAINAME",
                    AZ_1,
                    UPDATE_DAI_HOST_NOT_IN_AZ_DAIS);

    public static DeploymentSpec UPDATE_OPENSTACK_AZ_NOT_SELECTED_DS =
            createDsWithAvailabilityZoneSelectedData(
                    6L,
                    REGION_1,
                    "UPDATE_DAI_NOT_IN_AZ_HOSTNAME",
                    "UPDATE_DAI_NOT_IN_AZ_DAINAME",
                    "UPDATE_OS_AZ_NOT_SELECTED",
                    UPDATE_OPENSTACK_AZ_NOT_SELECTED_DAIS);

    public static DeploymentSpec UPDATE_HOST_AGGREGATE_SELECTED_DS =
            createDsWithHostAggregateSelectedData(
                    7L,
                    REGION_1,
                    "UPDATE_HOST_AGGREGATE_SELECTED_HA_ID");

    public static DeploymentSpec UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_DS =
            createDsWithDaiAndHostAggregateSelectedData(
                    8L,
                    REGION_1,
                    "UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_HOSTNAME",
                    "UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_DAINAME",
                    "UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_HA_ID",
                    UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_DAIS);

    public static DeploymentSpec DELETE_DS_WITHOUT_SG_REFERENCE =
            createDsForDeletionWithoutSgReferenceData(
                    9L,
                    "DELETE_DAINAME");

    public static DeploymentSpec DELETE_DS_WITH_SG_REFERENCE =
            createDsForDeletionWithSgReferenceData(
                    10L,
                    "DELETE_DAINAME");

    private static DeploymentSpec createAllHostsInRegionData (Long dsId, String region) {
        return createDeploymentSpec(dsId, region);
    }

    private static DeploymentSpec createDsWithDaiAndHostSelectedData (Long dsId, String region, String daiHostName, String selectedHostName, String daiName) {
        DeploymentSpec ds = createDeploymentSpec(dsId, region);
        DistributedApplianceInstance dai = new DistributedApplianceInstance(VIRTUALSYSTEM);
        dai.setOsHostName(daiHostName);
        dai.setName(daiName);

        ds.setDistributedApplianceInstances(new HashSet<>(Arrays.asList(dai)));

        Host host = new Host();
        host.setName(selectedHostName);
        ds.setHosts(new HashSet<>(Arrays.asList(host)));

        return ds;
    }

    private static DeploymentSpec createDsForDeletionWithoutSgReferenceData(Long dsId, String daiName) {
        DeploymentSpec ds = createDsWithDaiAndHostSelectedData(dsId, null, null, null, daiName);
        ds.setMarkedForDeletion(true);

        return ds;
    }

    private static DeploymentSpec createDsForDeletionWithSgReferenceData(Long dsId, String daiName) {
        DeploymentSpec ds = createDsWithDaiAndHostSelectedData(dsId, null, null, null, daiName);
        ds.setOsSecurityGroupReference(new OsSecurityGroupReference(null, null, null));
        ds.setMarkedForDeletion(true);

        return ds;
    }

    private static DeploymentSpec createDsWithAvailabilityZoneSelectedData (Long dsId, String region, String daiHostName, String daiName, String selectedAzName, List<DistributedApplianceInstance> dais) {
        DeploymentSpec ds = createDeploymentSpec(dsId, region);

        AvailabilityZone az = new AvailabilityZone(ds, null, selectedAzName);
        ds.setAvailabilityZones(new HashSet<>(Arrays.asList(az)));

        DistributedApplianceInstance dai = new DistributedApplianceInstance(VIRTUALSYSTEM);
        dai.setOsHostName(daiHostName);
        dai.setName(daiName);

        dais.add(dai);
        return ds;
    }

    private static DeploymentSpec createDsWithHostAggregateSelectedData(Long dsId, String region, String hostAggregateOSId) {
        DeploymentSpec ds = createDeploymentSpec(dsId, region);

        HostAggregate ha = new HostAggregate(ds, hostAggregateOSId);

        ds.setHostAggregates(new HashSet<>(Arrays.asList(ha)));
        return ds;
    }

    private static DeploymentSpec createDsWithDaiAndHostAggregateSelectedData(Long dsId, String region, String daiHostName, String daiName, String hostAggregateOSId, List<DistributedApplianceInstance> dais) {
        DeploymentSpec ds = createDsWithHostAggregateSelectedData(dsId, region, hostAggregateOSId);
        DistributedApplianceInstance dai = new DistributedApplianceInstance(VIRTUALSYSTEM);
        dai.setOsHostName(daiHostName);
        dai.setName(daiName);

        dais.add(dai);
        ds.setDistributedApplianceInstances(new HashSet<>(Arrays.asList(dai)));

        return ds;
    }

    public static TaskGraph createAllHostsInRegionGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new OsSvaCreateMetaTask(ds, HS_1_1, AZ_1));
        return expectedGraph;
    }

    public static TaskGraph createDAIHostSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(
                new OsDAIConformanceCheckMetaTask(
                        (DistributedApplianceInstance) ds.getDistributedApplianceInstances().toArray()[0],
                        true));
        return expectedGraph;
    }

    public static TaskGraph createDAIHostNotSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new DeleteSvaServerAndDAIMetaTask(ds.getRegion(),
                (DistributedApplianceInstance) ds.getDistributedApplianceInstances().toArray()[0]));

        expectedGraph.addTask(new OsSvaCreateMetaTask(ds, HS_1_1, AZ_1));

        return expectedGraph;
    }

    public static TaskGraph createDAIHostAggregateNotSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new DeleteSvaServerAndDAIMetaTask(ds.getRegion(),
                (DistributedApplianceInstance) ds.getDistributedApplianceInstances().toArray()[0]));

        return expectedGraph;
    }

    public static TaskGraph createAZSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new OsDAIConformanceCheckMetaTask((DistributedApplianceInstance) UPDATE_AZ_SELECTED_DAIS.toArray()[0], true));
        return expectedGraph;
    }

    public static TaskGraph createDaiHostNotInAZSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new OsDAIConformanceCheckMetaTask((DistributedApplianceInstance) UPDATE_DAI_HOST_NOT_IN_AZ_DAIS.toArray()[0], false));
        expectedGraph.addTask(new OsSvaCreateMetaTask(ds, HS_1_1, ((AvailabilityZone)ds.getAvailabilityZones().toArray()[0]).getZone()));
        return expectedGraph;
    }

    public static TaskGraph createOpenStackAZNotSelectedGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new OsDAIConformanceCheckMetaTask((DistributedApplianceInstance) UPDATE_OPENSTACK_AZ_NOT_SELECTED_DAIS.toArray()[0], false));
        expectedGraph.addTask(new DeleteSvaServerAndDAIMetaTask(ds.getRegion(), (DistributedApplianceInstance) UPDATE_OPENSTACK_AZ_NOT_SELECTED_DAIS.toArray()[0]));
        return expectedGraph;
    }

    public static TaskGraph createDeleteDsGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new DeleteSvaServerAndDAIMetaTask(ds.getRegion(), (DistributedApplianceInstance) ds.getDistributedApplianceInstances().toArray()[0]));
        if (ds.getOsSecurityGroupReference() != null) {
            expectedGraph.appendTask(new DeleteOsSecurityGroupTask(ds, ds.getOsSecurityGroupReference()));
        }
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask(ds.getVirtualSystem()));
        expectedGraph.appendTask(new DeleteDSFromDbTask(ds));
        return expectedGraph;
    }

    private static DeploymentSpec createDeploymentSpec(Long dsId, String region) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName("vc_name");
        VirtualSystem vs = new VirtualSystem(new DistributedAppliance());
        vs.setId(150L);
        vs.setVirtualizationConnector(vc);
        DeploymentSpec ds = new DeploymentSpec(vs, region, null, null,null, null);
        ds.setId(dsId);

        TEST_DEPLOYMENT_SPECS.add(ds);
        return ds;
    }
}
