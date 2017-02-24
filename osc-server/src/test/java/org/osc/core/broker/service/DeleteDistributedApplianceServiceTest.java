package org.osc.core.broker.service;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.request.DeleteDistributedApplianceRequestValidator;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteDAFromDbTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.ForceDeleteDATask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.ValidateNsxTask;
import org.osc.core.test.util.TaskGraphMatcher;
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

    @InjectMocks
    DeleteDistributedApplianceService deleteDistributedApplianceService;

    @Mock
    Session session;

    @Mock
    private DeleteDistributedApplianceRequestValidator validatorMock;

    private JobEngine jobEngine;

    // TODO:Future Supressing all unchecked warnings. Bad way to fix the warning for the line. Upgrade mockito to fix.
    // Mockito.when(LockUtil.tryLockDA(UNLOCKABLE_DA, null)).thenThrow(NullPointerException.class);
    @SuppressWarnings("unchecked")
    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        VALID_DA_WITH_SYSTEMS.setName("name");
        VirtualizationConnector openStackVirtualizationConnector = new VirtualizationConnector();
        openStackVirtualizationConnector.setVirtualizationType(VirtualizationType.OPENSTACK);
        VirtualSystem openStackVirtualSystem = new VirtualSystem();
        openStackVirtualSystem.setVirtualizationConnector(openStackVirtualizationConnector);
        openStackVirtualSystem.setId(101L);
        VirtualizationConnector vmWareVirtualizationConnector = new VirtualizationConnector();
        vmWareVirtualizationConnector.setVirtualizationType(VirtualizationType.VMWARE);
        VirtualSystem vmWareVirtualSystem = new VirtualSystem();
        vmWareVirtualSystem.setVirtualizationConnector(vmWareVirtualizationConnector);
        vmWareVirtualSystem.setId(102L);
        VALID_DA_WITH_SYSTEMS.addVirtualSystem(openStackVirtualSystem);
        VALID_DA_WITH_SYSTEMS.addVirtualSystem(vmWareVirtualSystem);

        Mockito.when(this.validatorMock.validateAndLoad(INVALID_REQUEST)).thenThrow(new VmidcBrokerValidationException(""));
        Mockito.when(this.validatorMock.validateAndLoad(VALID_REQUEST_FORCE_DELETE)).thenReturn(VALID_DA);
        Mockito.when(this.validatorMock.validateAndLoad(VALID_REQUEST_NOT_FORCE_DELETE)).thenReturn(VALID_DA_WITH_SYSTEMS);
        Mockito.when(this.validatorMock.validateAndLoad(UNLOCKABLE_DA_REQUEST)).thenReturn(UNLOCKABLE_DA);

        UnlockObjectMetaTask ult = new UnlockObjectMetaTask(null);
        PowerMockito.mockStatic(LockUtil.class);
        Mockito.when(LockUtil.tryLockDA(VALID_DA, null)).thenReturn(ult);
        Mockito.when(LockUtil.tryLockDA(VALID_DA_WITH_SYSTEMS, null)).thenReturn(ult);
        Mockito.when(LockUtil.tryLockDA(UNLOCKABLE_DA, null)).thenThrow(NullPointerException.class);

        PowerMockito.mockStatic(JobEngine.class);
        this.jobEngine = Mockito.mock(JobEngine.class);
        Job job = Mockito.mock(Job.class);

        TaskGraph taskGraphWithForceDeleteTask = new TaskGraph();
        taskGraphWithForceDeleteTask.addTask(new ForceDeleteDATask(VALID_DA));
        taskGraphWithForceDeleteTask.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);

        TaskGraph taskGraphWithDeleteTask = new TaskGraph();
        taskGraphWithDeleteTask.appendTask(new DeleteDAFromDbTask(VALID_DA), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        taskGraphWithDeleteTask.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);

        TaskGraph taskGraphWithDeleteTaskAndVsTasks = new TaskGraph();
        TaskGraph vmWareVsDeleteTaskGraph = new TaskGraph();
        vmWareVsDeleteTaskGraph.addTask(new ValidateNsxTask(vmWareVirtualSystem));
        vmWareVsDeleteTaskGraph.appendTask(new VSConformanceCheckMetaTask(vmWareVirtualSystem));
        taskGraphWithDeleteTaskAndVsTasks.addTaskGraph(vmWareVsDeleteTaskGraph);
        TaskGraph openStackVsDeleteTaskGraph = new TaskGraph();
        openStackVsDeleteTaskGraph.appendTask(new VSConformanceCheckMetaTask(openStackVirtualSystem));
        taskGraphWithDeleteTaskAndVsTasks.addTaskGraph(openStackVsDeleteTaskGraph);
        taskGraphWithDeleteTaskAndVsTasks.appendTask(new DeleteDAFromDbTask(VALID_DA_WITH_SYSTEMS), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
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
        this.deleteDistributedApplianceService.exec(INVALID_REQUEST, this.session);
    }

    @Test
    public void testExec_WhenLockingFails_ThrowsException() throws Exception {
        // Arrange
        this.exception.expect(NullPointerException.class);

        // Act
        this.deleteDistributedApplianceService.exec(UNLOCKABLE_DA_REQUEST, this.session);
    }

    @Test
    public void testExec_WithForceDeleteRequest_ExpectsSuccess() throws Exception {
        // Act
        BaseJobResponse baseJobResponse = this.deleteDistributedApplianceService.exec(VALID_REQUEST_FORCE_DELETE, this.session);

        // Assert
        Assert.assertEquals("The received JobID in force delete case is different than expected.", JOB_ID, baseJobResponse.getJobId());
        Mockito.verify(this.session, Mockito.times(0)).beginTransaction();
    }

    @Test
    public void testExec_WithNonForceDeleteRequestAndValidDA_ExpectsSuccess() throws Exception {
        // Act
        BaseJobResponse baseJobResponse = this.deleteDistributedApplianceService.exec(VALID_REQUEST_NOT_FORCE_DELETE, this.session);

        // Assert
        Assert.assertEquals("The received JobID in non force delete case is different than expected.", JOB_ID, baseJobResponse.getJobId());
        Mockito.verify(this.session, Mockito.times(1)).beginTransaction();
    }

    @Test
    public void testStartDeleteDAJob_WithoutUnlockObjectMetaTask_ExpectsSuccess() throws Exception {
        // Act
        DeleteDistributedApplianceService.startDeleteDAJob(VALID_DA, null);

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