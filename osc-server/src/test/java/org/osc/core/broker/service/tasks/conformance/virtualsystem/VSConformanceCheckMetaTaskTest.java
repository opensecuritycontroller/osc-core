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

import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DEFAULT_SERVICE_NAME;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DELETE_OPENSTACK_WITH_DEPLOYMENT_SPECS_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DELETE_OPENSTACK_WITH_OS_FLAVOR_REF_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DELETE_OPENSTACK_WITH_OS_IMAGE_REF_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DELETE_VMWARE_DELETE_SERVICE_INSTANCE_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DELETE_VMWARE_DELETE_SERVICE_MANAGER_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DELETE_VMWARE_DELETE_SERVICE_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.DELETE_VMWARE_UNDENPLOY_SERVICE_INSTANCE_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.TEST_VIRTUAL_SYSTEMS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_TASK;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_SERVICEINSTANCE_IP_OUT_OF_SYNC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_SERVICEINSTANCE_PASSWORD_OUT_OF_SYNC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_SERVICEMANAGER_NAME_OUT_OF_SYNC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_SERVICEMANAGER_URL_OUT_OF_SYNC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_SERVICE_IP_OUT_OF_SYNC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_SERVICE_NAME_OUT_OF_SYNC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_SERVICE_PASSWORD_OUT_OF_SYNC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_VSDOMAIN_POLICY_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_VSPOLICY_MARKED_FOR_DELETION_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_VSPOLICY_NAME_OUT_OF_SYNC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_VSPOLICY_WITHOUT_SERVICE_ID_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.UPDATE_VMWARE_VSPOLICY_WITHOUT_TEMPLATE_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createDeleteOpenStackWithDeploymentSpecGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createDeleteOpenStackWithOSFlavorRefGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createDeleteOpenStackWithOSImageRefGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createDeleteServiceGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createDeleteServiceInstanceGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createDeleteServiceManagerGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createDomainPolicyOnlyGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createOpenstacWhenLockingDeploymentSpecFailsGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createOpenstackNoDeploymentSpecGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createOpenstackWithDeploymentSpecGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createServiceInstanceOutOfSyncGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createServiceManagerOutOfSyncGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createServiceOutOfSyncGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createUndenployServiceInstanceGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createVsPolicyMarkedForDeletionGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createVsPolicyNameOutOfSyncGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createVsPolicyWithoutServiceIdGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.createVsPolicyWithoutTemplateGraph;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.encryption.EncryptionException;
import org.osc.sdk.sdn.api.ServiceApi;
import org.osc.sdk.sdn.api.ServiceManagerApi;
import org.osc.sdk.sdn.element.ServiceElement;
import org.osc.sdk.sdn.element.ServiceManagerElement;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({VMwareSdnApiFactory.class, LockUtil.class, CreateNsxServiceManagerTask.class})
@PowerMockIgnore("javax.net.ssl.*")
public class VSConformanceCheckMetaTaskTest {
    @Mock
    public EntityManager em;

    private ServiceManagerApi serviceManagerApiMock;
    private ServiceApi serviceApiMock;

    private static String DEFAULT_SERVICE_IP = getDefaultServerIp();
    private static String DEFAULT_SERVICEMANAGER_NAME = "DEFAULT_MANAGER_NAME";
    private static String DEFAULT_SERVICEMANAGER_URL = CreateNsxServiceManagerTask.buildRestCallbackUrl();
    private static String DEFAULT_SERVICEMANAGER_PASSWORD;

    private static String DEFAULT_SERVICE_PASSWORD;

    private VirtualSystem vs;

    private TaskGraph expectedGraph;

    public VSConformanceCheckMetaTaskTest(VirtualSystem vs, TaskGraph tg, boolean extraAutomation) {
        this.vs = vs;
        this.expectedGraph = tg;
    }

    @BeforeClass
    public static void testSuiteInitialize() throws EncryptionException {
        DEFAULT_SERVICEMANAGER_PASSWORD = EncryptionUtil.encryptAESCTR(AgentAuthFilter.VMIDC_AGENT_PASS);
        DEFAULT_SERVICE_PASSWORD = EncryptionUtil.encryptAESCTR(AgentAuthFilter.VMIDC_AGENT_PASS);
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
        this.serviceManagerApiMock = Mockito.mock(ServiceManagerApi.class);
        this.serviceApiMock = Mockito.mock(ServiceApi.class);

        for (VirtualSystem vs: TEST_VIRTUAL_SYSTEMS) {
            Mockito.doReturn(vs).when(this.em).find(VirtualSystem.class, vs.getId());
        }

        registerServiceManager(UPDATE_VMWARE_SERVICEMANAGER_NAME_OUT_OF_SYNC_VS.getNsxServiceManagerId(), "nameOutOfSync", DEFAULT_SERVICEMANAGER_URL, DEFAULT_SERVICEMANAGER_PASSWORD);
        registerServiceManager(UPDATE_VMWARE_SERVICEMANAGER_URL_OUT_OF_SYNC_VS.getNsxServiceManagerId(),DEFAULT_SERVICEMANAGER_NAME, "urlOutOfSync", DEFAULT_SERVICEMANAGER_PASSWORD);
        registerServiceManager(UPDATE_VMWARE_SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_VS.getNsxServiceManagerId(), DEFAULT_SERVICEMANAGER_NAME, DEFAULT_SERVICEMANAGER_URL, "passwordOutOfSync");

        registerService(UPDATE_VMWARE_SERVICE_NAME_OUT_OF_SYNC_VS.getNsxServiceId(), "nameOutOfSync", DEFAULT_SERVICE_IP, DEFAULT_SERVICE_PASSWORD);
        registerService(UPDATE_VMWARE_SERVICE_IP_OUT_OF_SYNC_VS.getNsxServiceId(), DEFAULT_SERVICE_NAME, "ipOutOfSync", DEFAULT_SERVICE_PASSWORD);
        registerService(UPDATE_VMWARE_SERVICE_PASSWORD_OUT_OF_SYNC_VS.getNsxServiceId(), DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_IP, "passwordOutOfSync");
        registerService(UPDATE_VMWARE_VSPOLICY_NAME_OUT_OF_SYNC_VS.getNsxServiceId(), DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_IP, DEFAULT_SERVICE_PASSWORD);

        PowerMockito.spy(CreateNsxServiceManagerTask.class);
        PowerMockito.doReturn(DEFAULT_SERVICEMANAGER_NAME).when(CreateNsxServiceManagerTask.class, "generateServiceManagerName", Mockito.any(VirtualSystem.class));

        PowerMockito.spy(LockUtil.class);
        PowerMockito.doReturn(UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_TASK).when(LockUtil.class, "tryLockDS",
                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getDeploymentSpecs().toArray()[0],
                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getDistributedAppliance(),
                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getDistributedAppliance().getApplianceManagerConnector(),
                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getVirtualizationConnector());

        PowerMockito.doThrow(new NullPointerException()).when(LockUtil.class, "tryLockDS",
                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getDeploymentSpecs().toArray()[0],
                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getDistributedAppliance(),
                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getDistributedAppliance().getApplianceManagerConnector(),
                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getVirtualizationConnector());

        PowerMockito.mockStatic(VMwareSdnApiFactory.class);
        Mockito.when(VMwareSdnApiFactory.createServiceManagerApi(UPDATE_VMWARE_SERVICEMANAGER_NAME_OUT_OF_SYNC_VS)).thenReturn(this.serviceManagerApiMock);
        Mockito.when(VMwareSdnApiFactory.createServiceManagerApi(UPDATE_VMWARE_SERVICEMANAGER_URL_OUT_OF_SYNC_VS)).thenReturn(this.serviceManagerApiMock);
        Mockito.when(VMwareSdnApiFactory.createServiceManagerApi(UPDATE_VMWARE_SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_VS)).thenReturn(this.serviceManagerApiMock);

        Mockito.when(VMwareSdnApiFactory.createServiceApi(UPDATE_VMWARE_SERVICE_NAME_OUT_OF_SYNC_VS)).thenReturn(this.serviceApiMock);
        Mockito.when(VMwareSdnApiFactory.createServiceApi(UPDATE_VMWARE_SERVICE_IP_OUT_OF_SYNC_VS)).thenReturn(this.serviceApiMock);
        Mockito.when(VMwareSdnApiFactory.createServiceApi(UPDATE_VMWARE_SERVICE_PASSWORD_OUT_OF_SYNC_VS)).thenReturn(this.serviceApiMock);
        Mockito.when(VMwareSdnApiFactory.createServiceApi(UPDATE_VMWARE_VSPOLICY_NAME_OUT_OF_SYNC_VS)).thenReturn(this.serviceApiMock);
    }


    @Test
    public void testExecuteTransaction_WithVariousVirtualSystems_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        VSConformanceCheckMetaTask task = new VSConformanceCheckMetaTask(this.vs);

        // Act.
        task.executeTransaction(this.em);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() throws EncryptionException {
        return Arrays.asList(new Object[][] {
            {UPDATE_VMWARE_SERVICEMANAGER_NAME_OUT_OF_SYNC_VS, createServiceManagerOutOfSyncGraph(UPDATE_VMWARE_SERVICEMANAGER_NAME_OUT_OF_SYNC_VS), false},
            {UPDATE_VMWARE_SERVICEMANAGER_URL_OUT_OF_SYNC_VS,  createServiceManagerOutOfSyncGraph(UPDATE_VMWARE_SERVICEMANAGER_URL_OUT_OF_SYNC_VS), false},
            {UPDATE_VMWARE_SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_VS,  createServiceManagerOutOfSyncGraph(UPDATE_VMWARE_SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_VS), false},
            {UPDATE_VMWARE_SERVICE_NAME_OUT_OF_SYNC_VS,  createServiceOutOfSyncGraph(UPDATE_VMWARE_SERVICE_NAME_OUT_OF_SYNC_VS), false},
            {UPDATE_VMWARE_SERVICE_IP_OUT_OF_SYNC_VS,  createServiceOutOfSyncGraph(UPDATE_VMWARE_SERVICE_IP_OUT_OF_SYNC_VS), false},
            {UPDATE_VMWARE_SERVICE_PASSWORD_OUT_OF_SYNC_VS,  createServiceOutOfSyncGraph(UPDATE_VMWARE_SERVICE_PASSWORD_OUT_OF_SYNC_VS), false},
            {UPDATE_VMWARE_SERVICEINSTANCE_PASSWORD_OUT_OF_SYNC_VS,  createServiceInstanceOutOfSyncGraph(UPDATE_VMWARE_SERVICEINSTANCE_PASSWORD_OUT_OF_SYNC_VS), false},
            {UPDATE_VMWARE_SERVICEINSTANCE_IP_OUT_OF_SYNC_VS,  createServiceInstanceOutOfSyncGraph(UPDATE_VMWARE_SERVICEINSTANCE_IP_OUT_OF_SYNC_VS), false},
            {UPDATE_VMWARE_VSPOLICY_MARKED_FOR_DELETION_VS,  createVsPolicyMarkedForDeletionGraph(UPDATE_VMWARE_VSPOLICY_MARKED_FOR_DELETION_VS), false},
            {UPDATE_VMWARE_VSPOLICY_WITHOUT_TEMPLATE_VS,  createVsPolicyWithoutTemplateGraph(UPDATE_VMWARE_VSPOLICY_WITHOUT_TEMPLATE_VS), false},
            {UPDATE_VMWARE_VSDOMAIN_POLICY_VS,  createDomainPolicyOnlyGraph(UPDATE_VMWARE_VSDOMAIN_POLICY_VS), false},
            {UPDATE_VMWARE_VSPOLICY_WITHOUT_SERVICE_ID_VS,  createVsPolicyWithoutServiceIdGraph(UPDATE_VMWARE_VSPOLICY_WITHOUT_SERVICE_ID_VS), false},
            {UPDATE_VMWARE_VSPOLICY_NAME_OUT_OF_SYNC_VS,  createVsPolicyNameOutOfSyncGraph(UPDATE_VMWARE_VSPOLICY_NAME_OUT_OF_SYNC_VS), false},
            {UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_VS,  createOpenstackNoDeploymentSpecGraph(UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_VS), false},
            {UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS,  createOpenstackWithDeploymentSpecGraph(UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS), false},
            {UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS,  createOpenstacWhenLockingDeploymentSpecFailsGraph(UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS), false},
            {DELETE_VMWARE_DELETE_SERVICE_INSTANCE_VS,  createDeleteServiceInstanceGraph(DELETE_VMWARE_DELETE_SERVICE_INSTANCE_VS), false},
            {DELETE_VMWARE_DELETE_SERVICE_MANAGER_VS,  createDeleteServiceManagerGraph(DELETE_VMWARE_DELETE_SERVICE_MANAGER_VS), false},
            {DELETE_VMWARE_DELETE_SERVICE_VS,  createDeleteServiceGraph(DELETE_VMWARE_DELETE_SERVICE_VS), false},
            {DELETE_VMWARE_UNDENPLOY_SERVICE_INSTANCE_VS,  createUndenployServiceInstanceGraph(DELETE_VMWARE_UNDENPLOY_SERVICE_INSTANCE_VS), true},
            {DELETE_OPENSTACK_WITH_DEPLOYMENT_SPECS_VS,  createDeleteOpenStackWithDeploymentSpecGraph(DELETE_OPENSTACK_WITH_DEPLOYMENT_SPECS_VS), false},
            {DELETE_OPENSTACK_WITH_OS_IMAGE_REF_VS,  createDeleteOpenStackWithOSImageRefGraph(DELETE_OPENSTACK_WITH_OS_IMAGE_REF_VS), false},
            {DELETE_OPENSTACK_WITH_OS_FLAVOR_REF_VS,  createDeleteOpenStackWithOSFlavorRefGraph(DELETE_OPENSTACK_WITH_OS_FLAVOR_REF_VS), false},
        });
    }

    private void registerServiceManager(String smId, String name, String url, String password) throws Exception {
        ServiceManagerElement serviceManager = Mockito.mock(ServiceManagerElement.class);
        Mockito.doReturn(name).when(serviceManager).getName();
        Mockito.doReturn(url).when(serviceManager).getCallbackUrl();
        Mockito.doReturn(password).when(serviceManager).getOscPassword();

        Mockito.doReturn(serviceManager).when(this.serviceManagerApiMock).getServiceManager(smId);
    }

    private void registerService(String sId, String name, String ip, String password) throws Exception {
        ServiceElement service = Mockito.mock(ServiceElement.class);
        Mockito.doReturn(name).when(service).getName();
        Mockito.doReturn(ip).when(service).getOscIpAddress();
        Mockito.doReturn(password).when(service).getOscPassword();

        Mockito.doReturn(service).when(this.serviceApiMock).getService(sId);
    }

    private static String getDefaultServerIp() {
        ServerUtil.setServerIP("127.0.0.1");
        return ServerUtil.getServerIP();
    }
}
