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

import static org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DSUpdateOrDeleteMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openstack4j.api.compute.HostService;
import org.openstack4j.model.compute.HostAggregate;
import org.openstack4j.model.compute.ext.AvailabilityZone;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteOsSecurityGroupTask;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;

@RunWith(Parameterized.class)
public class DSUpdateOrDeleteMetaTaskTest {

    public EntityManager em;

    @Mock
    public Openstack4JNova novaApiMock;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    DSUpdateOrDeleteMetaTask factoryTask;

    private DeploymentSpec ds;

    private TaskGraph expectedGraph;

    public DSUpdateOrDeleteMetaTaskTest(DeploymentSpec ds, TaskGraph tg) {
        this.ds = ds;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        populateDatabase();

        List<String> stringList = Arrays.asList(HS_1_1);
        List<AvailabilityZone> osAvailabilityZones1 = Arrays.asList(createAvailableZoneDetails(AZ_1, stringList));
        HostAggregate ha = Mockito.mock(HostAggregate.class);
        Mockito.doReturn(stringList).when(ha).getHosts();
        Mockito.doReturn("ha_name").when(ha).getName();
        Set<String> h1set = new HashSet<>(Arrays.asList(HS_1_1));
        Mockito.doReturn(osAvailabilityZones1).when(this.novaApiMock).getAvailabilityZonesDetail(REGION_1);
        Mockito.doReturn(h1set).when(this.novaApiMock).getComputeHosts(REGION_1);
        Mockito.doReturn(ha).when(this.novaApiMock).getHostAggregateById(
                REGION_1,
                ((org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate)UPDATE_HOST_AGGREGATE_SELECTED_DS.getHostAggregates().toArray()[0]).getOpenstackId());
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
       this.em.getTransaction().begin();

       this.em.persist(this.ds.getVirtualSystem()
               .getVirtualizationConnector());
       this.em.persist(this.ds.getVirtualSystem()
               .getDistributedAppliance().getApplianceManagerConnector());
       this.em.persist(this.ds.getVirtualSystem()
               .getDistributedAppliance().getAppliance());
       this.em.persist(this.ds.getVirtualSystem().getDistributedAppliance());
       this.em.persist(this.ds.getVirtualSystem().getApplianceSoftwareVersion());
       this.em.persist(this.ds.getVirtualSystem().getDomain());
       this.em.persist(this.ds.getVirtualSystem());
       for(DistributedApplianceInstance dai : this.ds.getVirtualSystem().getDistributedApplianceInstances()) {
           this.em.persist(dai);
       }

       if(this.ds.getOsSecurityGroupReference() != null) {
           this.em.persist(this.ds.getOsSecurityGroupReference());
       }

       this.em.persist(this.ds);


       // We have to do this crazy thing because HostAggregates
       // change their hashcodes once persisted...
       this.ds.getHostAggregates().clear();
       this.em.refresh(this.ds);

       this.em.getTransaction().commit();

    }
    @Test
    public void testExecuteTransaction_WithVariousDeploymentSpecs_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        this.factoryTask.osSvaCreateMetaTask = new OsSvaCreateMetaTask();
        this.factoryTask.osDAIConformanceCheckMetaTask = new OsDAIConformanceCheckMetaTask();
        this.factoryTask.mgrCheckDevicesMetaTask = new MgrCheckDevicesMetaTask();
        this.factoryTask.deleteSvaServerAndDAIMetaTask = new DeleteSvaServerAndDAIMetaTask();
        this.factoryTask.deleteOSSecurityGroup = new DeleteOsSecurityGroupTask();
        this.factoryTask.deleteDsFromDb = new DeleteDSFromDbTask();

        DSUpdateOrDeleteMetaTask task = this.factoryTask.create(this.ds, this.novaApiMock);

        // Act.
        task.execute();

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    private AvailabilityZone createAvailableZoneDetails(String azName, List<String> hostNames) {
        AvailabilityZone result = Mockito.mock(AvailabilityZone.class);
        Map<String, Map<String, HostService>> hosts = new HashMap<>();
        for(String hostName : hostNames) {
            hosts.put(hostName, null);
        }

        Mockito.doReturn(hosts).when(result).getHosts();
        Mockito.doReturn(azName).when(result).getZoneName();

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
