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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.common.job.TaskGuard;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteDAFromDbTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.ForceDeleteDATask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTask;
//import org.osc.core.broker.service.tasks.conformance.virtualsystem.ValidateNsxTask;
import org.osc.core.broker.service.validator.DeleteDistributedApplianceRequestValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TaskGraphMatcher;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LockUtil.class, JobEngine.class})
public class DeleteDistributedApplianceServiceTest {

    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = 2L;

    private static final Long JOB_ID = 11L;

    private static final BaseDeleteRequest INVALID_REQUEST = createRequest(INVALID_ID, false);
    private static final BaseDeleteRequest VALID_REQUEST_NOT_FORCE_DELETE = createRequest(VALID_ID, false);
    private static final BaseDeleteRequest VALID_REQUEST_FORCE_DELETE = createRequest(VALID_ID, true);
    private static final BaseDeleteRequest UNLOCKABLE_DA_REQUEST = createRequest(INVALID_ID, false);

    private static final DistributedAppliance VALID_DA = new DistributedAppliance();
    private static final DistributedAppliance VALID_DA_WITH_SYSTEMS = new DistributedAppliance();
    private static final DistributedAppliance UNLOCKABLE_DA = new DistributedAppliance();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    EntityManager em;

    @Mock
    EntityTransaction tx;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    private DeleteDistributedApplianceRequestValidator validatorMock;

    @Mock
    private ApiFactoryService apiFactoryService;

    @Mock
    private UserContextApi userContext;

    @Mock
    private DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    private VSConformanceCheckMetaTask vsConformanceCheckMetaTask;

    @InjectMocks
    private DeleteDistributedApplianceService deleteDistributedApplianceService;

    private JobEngine jobEngine;

    // TODO:Future Supressing all unchecked warnings. Bad way to fix the warning for the line. Upgrade mockito to fix.
    // Mockito.when(LockUtil.tryLockDA(UNLOCKABLE_DA, null)).thenThrow(NullPointerException.class);
    @SuppressWarnings("unchecked")
    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        // @InjectMocks does not inject these fields
        this.deleteDistributedApplianceService.vsConformanceCheckMetaTask = this.vsConformanceCheckMetaTask;
        this.deleteDistributedApplianceService.forceDeleteDATask = new ForceDeleteDATask();
        this.deleteDistributedApplianceService.deleteDAFromDbTask = new DeleteDAFromDbTask();

        Mockito.when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        VALID_DA_WITH_SYSTEMS.setName("name");
        VALID_DA_WITH_SYSTEMS.setId(VALID_ID);
        VirtualizationConnector openStackVirtualizationConnector = new VirtualizationConnector();
        openStackVirtualizationConnector.setVirtualizationType(VirtualizationType.OPENSTACK);
        VirtualSystem openStackVirtualSystem = new VirtualSystem();
        openStackVirtualSystem.setVirtualizationConnector(openStackVirtualizationConnector);
        openStackVirtualSystem.setId(101L);

        VALID_DA_WITH_SYSTEMS.addVirtualSystem(openStackVirtualSystem);

        Mockito.when(this.validatorMock.validateAndLoad(INVALID_REQUEST)).thenThrow(new VmidcBrokerValidationException(""));
        Mockito.when(this.validatorMock.validateAndLoad(VALID_REQUEST_FORCE_DELETE)).thenReturn(VALID_DA);
        Mockito.when(this.validatorMock.validateAndLoad(VALID_REQUEST_NOT_FORCE_DELETE)).thenReturn(VALID_DA_WITH_SYSTEMS);
        Mockito.when(this.validatorMock.validateAndLoad(UNLOCKABLE_DA_REQUEST)).thenReturn(UNLOCKABLE_DA);

        Mockito.when(this.em.find(DistributedAppliance.class, VALID_REQUEST_NOT_FORCE_DELETE.getId()))
            .thenReturn(VALID_DA_WITH_SYSTEMS);

        UnlockObjectMetaTask ult = new UnlockObjectMetaTask(null);
        PowerMockito.mockStatic(LockUtil.class);
        Mockito.when(LockUtil.tryLockDA(VALID_DA, null)).thenReturn(ult);
        Mockito.when(LockUtil.tryLockDA(VALID_DA_WITH_SYSTEMS, null)).thenReturn(ult);
        Mockito.when(LockUtil.tryLockDA(UNLOCKABLE_DA, null)).thenThrow(NullPointerException.class);

        PowerMockito.mockStatic(JobEngine.class);
        this.jobEngine = Mockito.mock(JobEngine.class);
        Job job = Mockito.mock(Job.class);

        TaskGraph taskGraphWithForceDeleteTask = new TaskGraph();
        taskGraphWithForceDeleteTask.addTask(new ForceDeleteDATask().create(VALID_DA));
        taskGraphWithForceDeleteTask.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);

        TaskGraph taskGraphWithDeleteTask = new TaskGraph();
        taskGraphWithDeleteTask.appendTask(new DeleteDAFromDbTask().create(VALID_DA), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        taskGraphWithDeleteTask.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);

        TaskGraph taskGraphWithDeleteTaskAndVsTasks = new TaskGraph();

        TaskGraph openStackVsDeleteTaskGraph = new TaskGraph();
        openStackVsDeleteTaskGraph.appendTask(this.vsConformanceCheckMetaTask.create(openStackVirtualSystem));
        taskGraphWithDeleteTaskAndVsTasks.addTaskGraph(openStackVsDeleteTaskGraph);
        taskGraphWithDeleteTaskAndVsTasks.appendTask(new DeleteDAFromDbTask().create(VALID_DA_WITH_SYSTEMS), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        taskGraphWithDeleteTaskAndVsTasks.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);

        Mockito.when(JobEngine.getEngine()).thenReturn(this.jobEngine);
        Mockito.when(this.jobEngine.submit(Matchers.eq("Force Delete Distributed Appliance '" + VALID_DA.getName() + "'"), Mockito.argThat(new TaskGraphMatcher(taskGraphWithForceDeleteTask)), Matchers.eq(LockObjectReference.getObjectReferences(VALID_DA)))).thenReturn(job);
        Mockito.when(this.jobEngine.submit(Matchers.eq("Delete Distributed Appliance '" + VALID_DA.getName() + "'"), Mockito.argThat(new TaskGraphMatcher(taskGraphWithDeleteTask)), Matchers.eq(LockObjectReference.getObjectReferences(VALID_DA)), Matchers.any(Job.JobCompletionListener.class))).thenReturn(job);
        Mockito.when(this.jobEngine.submit(Matchers.eq("Delete Distributed Appliance '" + VALID_DA_WITH_SYSTEMS.getName() + "'"), Mockito.argThat(new TaskGraphMatcher(taskGraphWithDeleteTaskAndVsTasks)), Matchers.eq(LockObjectReference.getObjectReferences(VALID_DA_WITH_SYSTEMS)), Matchers.any(Job.JobCompletionListener.class))).thenReturn(job);
        Mockito.when(job.getId()).thenReturn(JOB_ID);
    }

    @Test
    public void testExec_WhenValidationFails_ThrowsException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);

        // Act
        this.deleteDistributedApplianceService.dispatch(INVALID_REQUEST);
    }

    @Test
    public void testExec_WhenLockingFails_ThrowsException() throws Exception {
        // Arrange
        this.exception.expect(NullPointerException.class);

        // Act
        this.deleteDistributedApplianceService.dispatch(UNLOCKABLE_DA_REQUEST);
    }

    @Test
    public void testExec_WithForceDeleteRequest_ExpectsSuccess() throws Exception {
        // Act
        BaseJobResponse baseJobResponse = this.deleteDistributedApplianceService.dispatch(VALID_REQUEST_FORCE_DELETE);

        // Assert
        Assert.assertEquals("The received JobID in force delete case is different than expected.", JOB_ID, baseJobResponse.getJobId());
        Mockito.verify(this.tx, Mockito.times(1)).begin();
        Mockito.verify(this.tx, Mockito.times(1)).commit();
    }

    @Test
    public void testExec_WithNonForceDeleteRequestAndValidDA_ExpectsSuccess() throws Exception {
        // Act
        BaseJobResponse baseJobResponse = this.deleteDistributedApplianceService.dispatch(VALID_REQUEST_NOT_FORCE_DELETE);

        // Assert
        Assert.assertEquals("The received JobID in non force delete case is different than expected.", JOB_ID, baseJobResponse.getJobId());
        // The task commits part way then runs another transaction
        Mockito.verify(this.tx, Mockito.times(2)).begin();
        Mockito.verify(this.tx, Mockito.times(2)).commit();
    }

    @Test
    public void testStartDeleteDAJob_WithoutUnlockObjectMetaTask_ExpectsSuccess() throws Exception {
        // Act
        this.deleteDistributedApplianceService.startDeleteDAJob(VALID_DA, null);

        // Assert
        PowerMockito.verifyStatic(Mockito.times(1));
        LockUtil.tryLockDA(VALID_DA, null);
    }

    private static BaseDeleteRequest createRequest(Long id, boolean forceDelete) {
        BaseDeleteRequest request = new BaseDeleteRequest(id);
        request.setForceDelete(forceDelete);
        return request;
    }
}