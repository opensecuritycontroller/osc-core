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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BaseJobResponse;

@RunWith(Parameterized.class)
public class SyncDistributedApplianceServiceTest {
    private final static Long NO_SUCH_DA_ID = 1L;
    private final static Long MARKED_FOR_DELETE_DA_ID = 2L;
    private final static Long GOOD_DA_ID = 3L;

    private static final DistributedAppliance GOOD_DA = new DistributedAppliance();
    private static final DistributedAppliance MARKED_FOR_DELETE_DA = new DistributedAppliance();

    static {
        GOOD_DA.setId(GOOD_DA_ID);
        MARKED_FOR_DELETE_DA.setId(MARKED_FOR_DELETE_DA_ID);
        MARKED_FOR_DELETE_DA.setMarkedForDeletion(true);
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private DistributedApplianceConformJobFactory jobFactory;

    @Mock
    private EntityManager em;

    @InjectMocks
    SyncDistributedApplianceService service;

    private DistributedAppliance da;
    private Long daId;
    private Long jobId;

    public SyncDistributedApplianceServiceTest(Long daId, DistributedAppliance da,
                                               Long jobId, Class<? extends Throwable> expectedException) {
        this.da = da;
        this.daId = daId;
        this.jobId = jobId;

        if (expectedException != null) {
            this.exception.expect(expectedException);
        }
    }

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.em.find(eq(DistributedAppliance.class), eq(MARKED_FOR_DELETE_DA_ID)))
               .thenReturn(MARKED_FOR_DELETE_DA);
        when(this.em.find(eq(DistributedAppliance.class), eq(GOOD_DA_ID))).thenReturn(GOOD_DA);
        when(this.jobFactory.startDAConformJob(any(EntityManager.class), any(DistributedAppliance.class)))
               .thenAnswer(new Answer<Long>() {
                   @Override
                   public Long answer(InvocationOnMock invocation) throws Throwable {
                       DistributedAppliance result = invocation.getArgumentAt(1, DistributedAppliance.class);
                       return result.getId();
                   }
               });
    }

    @Test
    public void testExec_WithVariousIds() throws Exception {
        BaseIdRequest request = new BaseIdRequest(this.daId);
        BaseJobResponse response = this.service.exec(request, this.em);
        Assert.assertNotNull(response);
        assertEquals(this.jobId, response.getJobId());
    }

    @Parameters
    public static Object[][] data() {
        return new Object[][] {
            {GOOD_DA_ID, GOOD_DA, GOOD_DA_ID, null},
            {MARKED_FOR_DELETE_DA_ID, MARKED_FOR_DELETE_DA, null, VmidcBrokerInvalidRequestException.class},
            {NO_SUCH_DA_ID, null, null, VmidcBrokerValidationException.class},
        };
    }
}
