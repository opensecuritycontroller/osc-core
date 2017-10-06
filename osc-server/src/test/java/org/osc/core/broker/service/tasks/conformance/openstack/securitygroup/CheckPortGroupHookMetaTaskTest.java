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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.CheckPortGroupHookMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionHookElement;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({ HibernateUtil.class, OpenstackUtil.class })
public class CheckPortGroupHookMetaTaskTest {
    @Mock
    public EntityManager em;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    protected EntityTransaction tx;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @Mock
    public ApiFactoryService apiFactoryServiceMock;

    @Mock
    SdnRedirectionApi redirectionApi;

    @InjectMocks
    CheckPortGroupHookMetaTask factoryTask;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private SecurityGroupInterface sgi;

    private DistributedApplianceInstance dai;

    private TaskGraph expectedGraph;

    private String expectedExceptionMessage;

    private boolean isDeleteTg;

    public CheckPortGroupHookMetaTaskTest(SecurityGroupInterface sgi, DistributedApplianceInstance dai, TaskGraph tg, String expectedExceptionMessage,
            boolean isDeleteTg) {
        this.sgi = sgi;
        this.expectedGraph = tg;
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.isDeleteTg = isDeleteTg;
        this.dai = dai;
    }

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        populateDatabase();

        PowerMockito.mockStatic(OpenstackUtil.class);

        registerInspectionHook(mock(InspectionHookElement.class), SGI_WITH_INSPECTION_HOOK_WITHOUT_ASSIGNED_DAI);
        registerInspectionHook(mock(InspectionHookElement.class), SGI_WITH_INSPECTION_HOOK_WITH_ASSIGNED_DAI);
        registerInspectionHook(mock(InspectionHookElement.class), SGI_DELETED_WITH_INSPECTION_HOOK_WITHOUT_ASSIGNED_DAI);
        registerInspectionHook(mock(InspectionHookElement.class), SGI_DELETED_WITH_INSPECTION_HOOK_WITH_ASSIGNED_DAI);

        registerDomain(null, SGI_WITHOUT_ASSIGNED_DAI_DOMAIN_NOT_FOUND);

        String existingDomain = "EXISTING_DOMAIN";

        registerDomain(existingDomain, SGI_WITHOUT_ASSIGNED_DAI_FOUND_DEPLOYED_DAI);
        registerDeployedDAI(DAI_DEPLOYED, SGI_WITHOUT_ASSIGNED_DAI_FOUND_DEPLOYED_DAI, existingDomain);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
        this.sgi.setId(null);
        this.sgi.getVirtualSystem().setId(null);
    }

    @Test
    public void testExecuteTransaction_WithVariousSecurityGroupInterfaces_ExpectsValidTaskGraphOrException() throws Exception {
        // Arrange.
        this.factoryTask.allocateDai = new AllocateDAIWithSGIMembersTask();
        this.factoryTask.deallocateDai = new DeallocateDAIOfSGIMembersTask();
        this.factoryTask.createPortGroupHook = new CreatePortGroupHookTask();
        this.factoryTask.removePortGroupHook = new RemovePortGroupHookTask();

        CheckPortGroupHookMetaTask task = this.factoryTask.create(this.sgi, this.isDeleteTg);

        if (this.expectedExceptionMessage != null) {
            this.exception.expect(VmidcBrokerValidationException.class);
            this.exception.expectMessage(this.expectedExceptionMessage);
        }

        // Act.
        task.execute();

        // Assert.
        if (this.expectedGraph != null) {
            TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
        }
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            {
                SGI_WITHOUT_NET_ELEMENT_WITH_ASSIGNED_DAI,
                DAI_PROTECTING_PORT,
                createInspectionHookGraph(SGI_WITHOUT_NET_ELEMENT_WITH_ASSIGNED_DAI, DAI_PROTECTING_PORT),
                null,
                false
            },
            {
                SGI_K8S_WITHOUT_NET_ELEMENT_WITH_ASSIGNED_DAI,
                DAI_K8S_PROTECTING_PORT,
                createInspectionHookGraph(SGI_K8S_WITHOUT_NET_ELEMENT_WITH_ASSIGNED_DAI, DAI_K8S_PROTECTING_PORT),
                null,
                false
            },
            {
                SGI_WITH_INSPECTION_HOOK_WITHOUT_ASSIGNED_DAI,
                null,
                null,
                String.format("An inspection hook was found in the SDN controller but a DAI was not found assigned to the SGI %s.",
                        SGI_WITH_INSPECTION_HOOK_WITHOUT_ASSIGNED_DAI.getName()),
                false
            },
            {
                SGI_WITH_INSPECTION_HOOK_WITH_ASSIGNED_DAI,
                DAI_PROTECTING_PORT_EXISTING_HOOK,
                createEmptyGraph(),
                null,
                false
            },
            {
                SGI_WITHOUT_ASSIGNED_DAI_DOMAIN_NOT_FOUND,
                null,
                null,
                String.format("No router/domain was found attached to any of the networks of "
                        + "the member %s of the security group %s.",
                        SGI_WITHOUT_ASSIGNED_DAI_DOMAIN_NOT_FOUND.getSecurityGroup().getSecurityGroupMembers().iterator().next().getMemberName(),
                        SGI_WITHOUT_ASSIGNED_DAI_DOMAIN_NOT_FOUND.getSecurityGroup().getName()),
                false
            },
            {
                SGI_WITHOUT_ASSIGNED_DAI_FOUND_DEPLOYED_DAI,
                DAI_DEPLOYED,
                createInspectionHookGraph(SGI_WITHOUT_ASSIGNED_DAI_FOUND_DEPLOYED_DAI, DAI_DEPLOYED),
                null,
                false
            },
            {
                SGI_DELETED_WITH_INSPECTION_HOOK_WITH_ASSIGNED_DAI,
                DAI_DELETION_PROTECTING_PORT_EXISTING_HOOK,
                removeInspectionHookDeallocateDAIGraph(SGI_DELETED_WITH_INSPECTION_HOOK_WITH_ASSIGNED_DAI, DAI_DELETION_PROTECTING_PORT_EXISTING_HOOK),
                null,
                false
            },
            {
                SGI_DELETED_WITH_INSPECTION_HOOK_WITHOUT_ASSIGNED_DAI,
                null,
                removeInspectionHookGraph(SGI_DELETED_WITH_INSPECTION_HOOK_WITHOUT_ASSIGNED_DAI),
                null,
                true
            }
        });
    }

    private void registerNetworkRedirectionApi(SdnRedirectionApi redirectionApi, VirtualSystem vs) throws Exception {
        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(vs)).thenReturn(redirectionApi);
    }

    private void registerInspectionHook(InspectionHookElement inspectionHook, SecurityGroupInterface sgi) throws Exception {
        when(this.redirectionApi.getInspectionHook(sgi.getNetworkElementId())).thenReturn(inspectionHook);
        registerNetworkRedirectionApi(this.redirectionApi, sgi.getVirtualSystem());
    }

    private void registerDomain(String domainId, SecurityGroupInterface sgi) throws Exception {
        PowerMockito.doReturn(domainId).
        when(OpenstackUtil.class, "extractDomainId",
                eq(sgi.getSecurityGroup().getProjectId()),
                eq(sgi.getSecurityGroup().getVirtualizationConnector().getProviderAdminProjectName()),
                Mockito.any(),
                Mockito.any());
    }

    private void registerDeployedDAI(DistributedApplianceInstance dai, SecurityGroupInterface sgi, String domainId) throws Exception {
        PowerMockito.doReturn(dai).
        when(OpenstackUtil.class, "findDeployedDAI",
                this.em,
                sgi.getVirtualSystem(),
                sgi.getSecurityGroup(),
                sgi.getSecurityGroup().getProjectId(),
                sgi.getSecurityGroup().getSecurityGroupMembers().iterator().next().getVm().getRegion(),
                domainId,
                sgi.getSecurityGroup().getSecurityGroupMembers().iterator().next().getVm().getHost(),
                false);
    }

    private void populateDatabase() {
        this.em.getTransaction().begin();
        if (this.sgi.getVirtualSystem().getId() == null) {
            this.em.persist(this.sgi.getVirtualSystem()
                    .getVirtualizationConnector());
            this.em.persist(this.sgi.getVirtualSystem()
                    .getDistributedAppliance().getApplianceManagerConnector());
            this.em.persist(this.sgi.getVirtualSystem()
                    .getDistributedAppliance().getAppliance());
            this.em.persist(this.sgi.getVirtualSystem().getDistributedAppliance());
            this.em.persist(this.sgi.getVirtualSystem().getApplianceSoftwareVersion());
            this.em.persist(this.sgi.getVirtualSystem().getDomain());
            this.em.persist(this.sgi.getVirtualSystem());
        }

        this.em.persist(this.sgi.getSecurityGroup());

        this.em.persist(this.sgi);

        Set<VMPort> protectedPorts = new HashSet<VMPort>();
        for (SecurityGroupMember sgm : this.sgi.getSecurityGroup().getSecurityGroupMembers()) {
            this.em.persist(sgm);

            if (sgm.getVm() != null) {
                this.em.persist(sgm.getVm());
                protectedPorts.addAll(sgm.getVm().getPorts());
            }

            if (sgm.getNetwork() != null) {
                this.em.persist(sgm.getNetwork());
                protectedPorts.addAll(sgm.getNetwork().getPorts());
            }

            if (sgm.getSubnet() != null) {
                this.em.persist(sgm.getSubnet());
                protectedPorts.addAll(sgm.getSubnet().getPorts());
            }
        }

        for (VMPort port : protectedPorts) {
            this.em.persist(port);
        }

        if (this.dai != null) {
            this.em.persist(this.dai.getDeploymentSpec());
            this.em.persist(this.dai);
        }

        this.em.refresh(this.sgi);

        this.em.getTransaction().commit();
    }
}
