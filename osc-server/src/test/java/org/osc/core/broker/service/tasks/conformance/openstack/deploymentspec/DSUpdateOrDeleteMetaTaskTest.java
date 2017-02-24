package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import static org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DSUpdateOrDeleteMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.jclouds.openstack.nova.v2_0.domain.HostAggregate;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.AvailabilityZoneDetails;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.AvailabilityZoneDetails.HostService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.util.SessionStub;
import org.osc.core.test.util.TaskGraphHelper;

import com.google.gwt.thirdparty.guava.common.collect.Sets;

@RunWith(Parameterized.class)
public class DSUpdateOrDeleteMetaTaskTest {
    @Mock
    public Session sessionMock;

    @Mock
    public JCloudNova novaApiMock;

    private DeploymentSpec ds;

    private TaskGraph expectedGraph;

    private SessionStub sessionStub;

    public DSUpdateOrDeleteMetaTaskTest(DeploymentSpec ds, TaskGraph tg) {
        this.ds = ds;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
        this.sessionStub = new SessionStub(this.sessionMock);

        for (DeploymentSpec ds: TEST_DEPLOYMENT_SPECS) {
            Mockito.doReturn(ds).when(this.sessionMock).get(DeploymentSpec.class, ds.getId());
        }

        List<AvailabilityZoneDetails> osAvailabilityZones1 = Arrays.asList(createAvailableZoneDetails(AZ_1, Arrays.asList(HS_1_1)));
        HostAggregate ha = Mockito.mock(HostAggregate.class);
        Mockito.doReturn(Sets.newHashSet(HS_1_1)).when(ha).getHosts();
        Mockito.doReturn("ha_name").when(ha).getName();

        Mockito.doReturn(osAvailabilityZones1).when(this.novaApiMock).getAvailabilityZonesDetail(REGION_1);
        Mockito.doReturn(Sets.newHashSet(HS_1_1)).when(this.novaApiMock).getComputeHosts(REGION_1);
        Mockito.doReturn(ha).when(this.novaApiMock).getHostAggregateById(
                REGION_1,
                ((org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate)UPDATE_HOST_AGGREGATE_SELECTED_DS.getHostAggregates().toArray()[0]).getOpenstackId());

        this.sessionStub.listByDsIdAndAvailabilityZone(
                UPDATE_AZ_SELECTED_DS.getId(),
                ((AvailabilityZone)UPDATE_AZ_SELECTED_DS.getAvailabilityZones().toArray()[0]).getZone(),
                UPDATE_AZ_SELECTED_DAIS);

        this.sessionStub.listByDsIdAndAvailabilityZone(
                UPDATE_DAI_HOST_NOT_IN_AZ_DS.getId(),
                ((AvailabilityZone)UPDATE_DAI_HOST_NOT_IN_AZ_DS.getAvailabilityZones().toArray()[0]).getZone(),
                UPDATE_DAI_HOST_NOT_IN_AZ_DAIS);

        this.sessionStub.listByDsIdAndAvailabilityZone(
                UPDATE_OPENSTACK_AZ_NOT_SELECTED_DS.getId(),
                ((AvailabilityZone)UPDATE_OPENSTACK_AZ_NOT_SELECTED_DS.getAvailabilityZones().toArray()[0]).getZone(),
                UPDATE_OPENSTACK_AZ_NOT_SELECTED_DAIS);
    }

    @Test
    public void testExecuteTransaction_WithVariousDeploymentSpecs_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        DSUpdateOrDeleteMetaTask task = new DSUpdateOrDeleteMetaTask(this.ds, this.novaApiMock);

        // Act.
        task.executeTransaction(this.sessionMock);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    private AvailabilityZoneDetails createAvailableZoneDetails(String azName, List<String> hostNames) {
        AvailabilityZoneDetails result = Mockito.mock(AvailabilityZoneDetails.class);
        Map<String, Map<String, HostService>> hosts = new HashMap<String, Map<String, HostService>>();
        for(String hostName : hostNames) {
            hosts.put(hostName, null);
        }

        Mockito.doReturn(hosts).when(result).getHosts();
        Mockito.doReturn(azName).when(result).getName();

        return result;
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            {UPDATE_NO_HOST_SELECTED_DS, createAllHostsInRegionGraph(UPDATE_NO_HOST_SELECTED_DS)},
            {UPDATE_DAI_HOST_SELECTED_DS, createDAIHostSelectedGraph(UPDATE_DAI_HOST_SELECTED_DS)},
            {UPDATE_DAI_HOST_NOT_SELECTED_DS, createDAIHostNotSelectedGraph(UPDATE_DAI_HOST_NOT_SELECTED_DS)},
            {UPDATE_AZ_SELECTED_DS, createAZSelectedGraph(UPDATE_AZ_SELECTED_DS)},
            {UPDATE_DAI_HOST_NOT_IN_AZ_DS, createDaiHostNotInAZSelectedGraph(UPDATE_DAI_HOST_NOT_IN_AZ_DS)},
            {UPDATE_OPENSTACK_AZ_NOT_SELECTED_DS, createOpenStackAZNotSelectedGraph(UPDATE_OPENSTACK_AZ_NOT_SELECTED_DS)},
            {UPDATE_HOST_AGGREGATE_SELECTED_DS, createAllHostsInRegionGraph(UPDATE_HOST_AGGREGATE_SELECTED_DS)},
            {UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_DS, createDAIHostAggregateNotSelectedGraph(UPDATE_DAI_HOST_AGGREGATE_NOT_SELECTED_DS)},
            {DELETE_DS_WITH_SG_REFERENCE, createDeleteDsGraph(DELETE_DS_WITH_SG_REFERENCE)},
            {DELETE_DS_WITHOUT_SG_REFERENCE, createDeleteDsGraph(DELETE_DS_WITHOUT_SG_REFERENCE)}
        });
    }
}
