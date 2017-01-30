package org.osc.core.broker.service;

import java.util.List;

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
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.UpdateDaiConsolePasswordRequest;
import org.osc.core.broker.service.request.UpdateDaiConsolePasswordRequestValidator;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.agent.UpdateApplianceConsolePasswordsMetaTask;
import org.osc.core.test.util.TaskGraphMatcher;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobEngine.class})
public class UpdateApplianceConsolePasswordServiceTest {

    private static final String VS_NAME = "name";

    private static final Long JOB_ID = 1L;

    private static final UpdateDaiConsolePasswordRequest INVALID_REQUEST = new UpdateDaiConsolePasswordRequest();
    private static final UpdateDaiConsolePasswordRequest VALID_REQUEST = new UpdateDaiConsolePasswordRequest();


    @Rule
    public ExpectedException exception = ExpectedException.none();

    @InjectMocks
    UpdateApplianceConsolePasswordService updateApplianceConsolePasswordService;

    @Mock
    Session session;

    @Mock
    private UpdateDaiConsolePasswordRequestValidator validatorMock;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.when(this.validatorMock.validateAndLoadList(INVALID_REQUEST)).thenThrow(new VmidcBrokerValidationException(""));
        VALID_REQUEST.setVsName(VS_NAME);

        TaskGraph tg = new TaskGraph();
        List<DistributedApplianceInstance> daiList = Lists.newArrayList();
        tg.addTask(new UpdateApplianceConsolePasswordsMetaTask(VALID_REQUEST.getNewPassword(), daiList));

        PowerMockito.mockStatic(JobEngine.class);
        JobEngine jobEngine = Mockito.mock(JobEngine.class);
        Job job = Mockito.mock(Job.class);
        Mockito.when(JobEngine.getEngine()).thenReturn(jobEngine);
        Mockito.when(jobEngine.submit(
                Mockito.eq("Update Appliance(s) console password for Virtual System: '" + VS_NAME + "'"),
                Mockito.argThat(new TaskGraphMatcher(tg)), Matchers.anySetOf(LockObjectReference.class))).thenReturn(job);
        Mockito.when(job.getId()).thenReturn(JOB_ID);
    }

    @Test
    public void testExec_WithInvalidRequest_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);

        // Act
        this.updateApplianceConsolePasswordService.exec(INVALID_REQUEST, this.session);
    }

    @Test
    public void testExec_WithValidRequest_ExpectsSuccess() throws Exception {
        // Act
        BaseJobResponse response = this.updateApplianceConsolePasswordService.exec(VALID_REQUEST, this.session);

        // Assert
        Assert.assertEquals("The received JobID is different than expected.", JOB_ID, response.getJobId());
    }
}