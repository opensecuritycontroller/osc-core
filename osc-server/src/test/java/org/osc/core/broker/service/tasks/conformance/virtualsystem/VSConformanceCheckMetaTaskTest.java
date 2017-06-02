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
//TODO Hailee: Commented code
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.tasks.conformance.GenerateVSSKeysTask;
//import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteServiceInstanceTask;
//import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteServiceManagerTask;
//import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteServiceTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteVsFromDbTask;
//import org.osc.core.broker.service.tasks.conformance.deleteda.UnregisterServiceManagerCallbackTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrDeleteVSSDeviceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteFlavorTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteImageFromGlanceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DSConformanceCheckMetaTask;
//import org.osc.core.broker.service.tasks.conformance.securitygroup.NsxSecurityGroupsCheckMetaTask;
//import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.NsxSecurityGroupInterfacesCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.SecurityGroupCleanupCheckMetaTask;
//import org.osc.core.broker.service.tasks.network.UpdateNsxServiceInstanceAttributesTask;
//import org.osc.core.broker.service.tasks.network.UpdateNsxServiceManagerTask;
//import org.osc.core.broker.service.tasks.passwordchange.UpdateNsxServiceAttributesTask;
import org.osc.core.broker.util.PasswordUtil;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.util.ServerUtil;
//import org.osc.sdk.sdn.api.ServiceApi;
//import org.osc.sdk.sdn.api.ServiceManagerApi;
//import org.osc.sdk.sdn.element.ServiceElement;
//import org.osc.sdk.sdn.element.ServiceManagerElement;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
//@PrepareForTest({LockUtil.class, CreateNsxServiceManagerTask.class, StaticRegistry.class})
@PowerMockIgnore("javax.net.ssl.*")
public class VSConformanceCheckMetaTaskTest {

    private static final String ENCRYPTED_PASSWORD = "encrypted";

    @Mock
    public EntityManager em;

    // This is initialised to create the test data
    private static EncryptionApi encryption;

    // this is initialised from getTestData(); otherwise the TaskNodeComparer fails
    private static ApiFactoryService apiFactoryService;

//    @InjectMocks
//    private CreateNsxServiceManagerTask createNsxServiceManagerTask;
//
//    @InjectMocks
//    private UpdateNsxServiceManagerTask updateNsxServiceManagerTask;

    @InjectMocks
    private VSConformanceCheckMetaTask vsConformanceCheckMetaTask;

//    @InjectMocks
//    private CreateNsxServiceTask createNsxServiceTask;
//
//    @InjectMocks
//    private NsxDeploymentSpecCheckMetaTask nsxDeploymentSpecCheckMetaTask;

    @InjectMocks
    private PasswordUtil passwordUtil;

//    @InjectMocks
//    private UpdateNsxServiceAttributesTask updateNsxServiceAttributesTask;
//
//    @InjectMocks
//    private UpdateNsxServiceInstanceAttributesTask updateNsxServiceInstanceAttributesTask;

    @InjectMocks
    private MgrCheckDevicesMetaTask mgrCheckDevicesMetaTask;

    @InjectMocks
    private DSConformanceCheckMetaTask dsConformanceCheckMetaTask;

//    @InjectMocks
//    private ValidateNsxAgentsTask validateNsxAgentsTask;

    @InjectMocks
    private MgrDeleteVSSDeviceTask mgrDeleteVSSDeviceTask;

//    @InjectMocks
//    private UpdateVendorTemplateTask updateVendorTemplateTask;
//
//    private ServiceManagerApi serviceManagerApiMock;
//    private ServiceApi serviceApiMock;

    private static String DEFAULT_SERVICE_IP = getDefaultServerIp();
    private static String DEFAULT_SERVICEMANAGER_NAME = "DEFAULT_MANAGER_NAME";
//    private static String DEFAULT_SERVICEMANAGER_URL = CreateNsxServiceManagerTask.buildRestCallbackUrl();

    private VirtualSystem vs;

    private TaskGraph expectedGraph;

    public VSConformanceCheckMetaTaskTest(VirtualSystem vs, TaskGraph tg, boolean extraAutomation) {
        this.vs = vs;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        // @InjectMocks doesn't inject these fields
//        this.vsConformanceCheckMetaTask.createNsxServiceManagerTask = this.createNsxServiceManagerTask;
//        this.vsConformanceCheckMetaTask.updateNsxServiceManagerTask = this.updateNsxServiceManagerTask;
//        this.vsConformanceCheckMetaTask.createNsxServiceTask = this.createNsxServiceTask;
//        this.vsConformanceCheckMetaTask.nsxDeploymentSpecCheckMetaTask = this.nsxDeploymentSpecCheckMetaTask;
        this.vsConformanceCheckMetaTask.passwordUtil = this.passwordUtil;
        this.vsConformanceCheckMetaTask.encryption = this.encryption;
//        this.vsConformanceCheckMetaTask.updateNsxServiceAttributesTask = this.updateNsxServiceAttributesTask;
//        this.vsConformanceCheckMetaTask.updateNsxServiceInstanceAttributesTask = this.updateNsxServiceInstanceAttributesTask;
        this.vsConformanceCheckMetaTask.mgrCheckDevicesMetaTask = this.mgrCheckDevicesMetaTask;
        this.vsConformanceCheckMetaTask.dsConformanceCheckMetaTask = this.dsConformanceCheckMetaTask;
//        this.vsConformanceCheckMetaTask.validateNsxAgentsTask = this.validateNsxAgentsTask;
        this.vsConformanceCheckMetaTask.mgrDeleteVSSDeviceTask = this.mgrDeleteVSSDeviceTask;
//        this.vsConformanceCheckMetaTask.updateVendorTemplateTask = this.updateVendorTemplateTask;
//        this.vsConformanceCheckMetaTask.registerServiceInstanceTask = new RegisterServiceInstanceTask();
        this.vsConformanceCheckMetaTask.generateVSSKeysTask = new GenerateVSSKeysTask();
//        this.vsConformanceCheckMetaTask.nsxSecurityGroupInterfacesCheckMetaTask = new NsxSecurityGroupInterfacesCheckMetaTask();
//        this.vsConformanceCheckMetaTask.nsxSecurityGroupsCheckMetaTask = new NsxSecurityGroupsCheckMetaTask();
//        this.vsConformanceCheckMetaTask.deleteDefaultServiceProfileTask = new DeleteDefaultServiceProfileTask();
//        this.vsConformanceCheckMetaTask.removeVendorTemplateTask = new RemoveVendorTemplateTask();
//        this.vsConformanceCheckMetaTask.deleteServiceInstanceTask = new DeleteServiceInstanceTask();
//        this.vsConformanceCheckMetaTask.registerVendorTemplateTask = new RegisterVendorTemplateTask();
//        this.vsConformanceCheckMetaTask.deleteServiceTask = new DeleteServiceTask();
//        this.vsConformanceCheckMetaTask.unregisterServiceManagerCallbackTask = new UnregisterServiceManagerCallbackTask();
        this.vsConformanceCheckMetaTask.securityGroupCleanupCheckMetaTask = new SecurityGroupCleanupCheckMetaTask();
        this.vsConformanceCheckMetaTask.deleteVsFromDbTask = new DeleteVsFromDbTask();
        this.vsConformanceCheckMetaTask.deleteFlavorTask = new DeleteFlavorTask();
//        this.vsConformanceCheckMetaTask.deleteServiceManagerTask = new DeleteServiceManagerTask();
        this.vsConformanceCheckMetaTask.deleteImageFromGlanceTask = new DeleteImageFromGlanceTask();

//        this.serviceManagerApiMock = Mockito.mock(ServiceManagerApi.class);
//        this.serviceApiMock = Mockito.mock(ServiceApi.class);

//        for (VirtualSystem vs: TEST_VIRTUAL_SYSTEMS) {
//            Mockito.doReturn(vs).when(this.em).find(VirtualSystem.class, vs.getId());
//        }

//        registerServiceManager(UPDATE_VMWARE_SERVICEMANAGER_NAME_OUT_OF_SYNC_VS.getNsxServiceManagerId(), "nameOutOfSync", DEFAULT_SERVICEMANAGER_URL, ENCRYPTED_PASSWORD);
//        registerServiceManager(UPDATE_VMWARE_SERVICEMANAGER_URL_OUT_OF_SYNC_VS.getNsxServiceManagerId(),DEFAULT_SERVICEMANAGER_NAME, "urlOutOfSync", ENCRYPTED_PASSWORD);
//        registerServiceManager(UPDATE_VMWARE_SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_VS.getNsxServiceManagerId(), DEFAULT_SERVICEMANAGER_NAME, DEFAULT_SERVICEMANAGER_URL, "passwordOutOfSync");
//
//        registerService(UPDATE_VMWARE_SERVICE_NAME_OUT_OF_SYNC_VS.getNsxServiceId(), "nameOutOfSync", DEFAULT_SERVICE_IP, ENCRYPTED_PASSWORD);
//        registerService(UPDATE_VMWARE_SERVICE_IP_OUT_OF_SYNC_VS.getNsxServiceId(), DEFAULT_SERVICE_NAME, "ipOutOfSync", ENCRYPTED_PASSWORD);
//        registerService(UPDATE_VMWARE_SERVICE_PASSWORD_OUT_OF_SYNC_VS.getNsxServiceId(), DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_IP, "passwordOutOfSync");
//        registerService(UPDATE_VMWARE_VSPOLICY_NAME_OUT_OF_SYNC_VS.getNsxServiceId(), DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_IP, ENCRYPTED_PASSWORD);

        Mockito.when(this.apiFactoryService.generateServiceManagerName(Mockito.any(VirtualSystem.class))).thenReturn(DEFAULT_SERVICEMANAGER_NAME);

        PowerMockito.spy(LockUtil.class);
//        PowerMockito.doReturn(UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_TASK).when(LockUtil.class, "tryLockDS",
//                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getDeploymentSpecs().toArray()[0],
//                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getDistributedAppliance(),
//                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getDistributedAppliance().getApplianceManagerConnector(),
//                UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS.getVirtualizationConnector());
//
//        PowerMockito.doThrow(new NullPointerException()).when(LockUtil.class, "tryLockDS",
//                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getDeploymentSpecs().toArray()[0],
//                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getDistributedAppliance(),
//                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getDistributedAppliance().getApplianceManagerConnector(),
//                UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS.getVirtualizationConnector());

//        Mockito.when(apiFactoryService.createServiceManagerApi(UPDATE_VMWARE_SERVICEMANAGER_NAME_OUT_OF_SYNC_VS)).thenReturn(this.serviceManagerApiMock);
//        Mockito.when(apiFactoryService.createServiceManagerApi(UPDATE_VMWARE_SERVICEMANAGER_URL_OUT_OF_SYNC_VS)).thenReturn(this.serviceManagerApiMock);
//        Mockito.when(apiFactoryService.createServiceManagerApi(UPDATE_VMWARE_SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_VS)).thenReturn(this.serviceManagerApiMock);
//
//        Mockito.when(apiFactoryService.createServiceApi(UPDATE_VMWARE_SERVICE_NAME_OUT_OF_SYNC_VS)).thenReturn(this.serviceApiMock);
//        Mockito.when(apiFactoryService.createServiceApi(UPDATE_VMWARE_SERVICE_IP_OUT_OF_SYNC_VS)).thenReturn(this.serviceApiMock);
//        Mockito.when(apiFactoryService.createServiceApi(UPDATE_VMWARE_SERVICE_PASSWORD_OUT_OF_SYNC_VS)).thenReturn(this.serviceApiMock);
//        Mockito.when(apiFactoryService.createServiceApi(UPDATE_VMWARE_VSPOLICY_NAME_OUT_OF_SYNC_VS)).thenReturn(this.serviceApiMock);

        PowerMockito.mockStatic(StaticRegistry.class);
        Mockito.when(StaticRegistry.encryptionApi()).thenReturn(encryption);
    }


    @Test
    public void testExecuteTransaction_WithVariousVirtualSystems_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        VSConformanceCheckMetaTask task = this.vsConformanceCheckMetaTask.create(this.vs);

        // Act.
        task.executeTransaction(this.em);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() throws EncryptionException {
        encryption = Mockito.mock(EncryptionApi.class);

        PowerMockito.mockStatic(StaticRegistry.class);
        Mockito.when(StaticRegistry.encryptionApi()).thenReturn(encryption);

        Mockito.when(encryption.encryptAESCTR("")).thenReturn(ENCRYPTED_PASSWORD);

//        apiFactoryService = VSConformanceCheckMetaTaskTestData.apiFactoryService;

        return Arrays.asList(new Object[][] {
//            {UPDATE_VMWARE_SERVICEMANAGER_NAME_OUT_OF_SYNC_VS, createServiceManagerOutOfSyncGraph(UPDATE_VMWARE_SERVICEMANAGER_NAME_OUT_OF_SYNC_VS), false},
//            {UPDATE_VMWARE_SERVICEMANAGER_URL_OUT_OF_SYNC_VS,  createServiceManagerOutOfSyncGraph(UPDATE_VMWARE_SERVICEMANAGER_URL_OUT_OF_SYNC_VS), false},
//            {UPDATE_VMWARE_SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_VS,  createServiceManagerOutOfSyncGraph(UPDATE_VMWARE_SERVICEMANAGER_PASSWORD_OUT_OF_SYNC_VS), false},
//            {UPDATE_VMWARE_SERVICE_NAME_OUT_OF_SYNC_VS,  createServiceOutOfSyncGraph(UPDATE_VMWARE_SERVICE_NAME_OUT_OF_SYNC_VS), false},
//            {UPDATE_VMWARE_SERVICE_IP_OUT_OF_SYNC_VS,  createServiceOutOfSyncGraph(UPDATE_VMWARE_SERVICE_IP_OUT_OF_SYNC_VS), false},
//            {UPDATE_VMWARE_SERVICE_PASSWORD_OUT_OF_SYNC_VS,  createServiceOutOfSyncGraph(UPDATE_VMWARE_SERVICE_PASSWORD_OUT_OF_SYNC_VS), false},
//            {UPDATE_VMWARE_SERVICEINSTANCE_PASSWORD_OUT_OF_SYNC_VS,  createServiceInstanceOutOfSyncGraph(UPDATE_VMWARE_SERVICEINSTANCE_PASSWORD_OUT_OF_SYNC_VS), false},
//            {UPDATE_VMWARE_SERVICEINSTANCE_IP_OUT_OF_SYNC_VS,  createServiceInstanceOutOfSyncGraph(UPDATE_VMWARE_SERVICEINSTANCE_IP_OUT_OF_SYNC_VS), false},
//            {UPDATE_VMWARE_VSPOLICY_MARKED_FOR_DELETION_VS,  createVsPolicyMarkedForDeletionGraph(UPDATE_VMWARE_VSPOLICY_MARKED_FOR_DELETION_VS), false},
//            {UPDATE_VMWARE_VSPOLICY_WITHOUT_TEMPLATE_VS,  createVsPolicyWithoutTemplateGraph(UPDATE_VMWARE_VSPOLICY_WITHOUT_TEMPLATE_VS), false},
//            {UPDATE_VMWARE_VSDOMAIN_POLICY_VS,  createDomainPolicyOnlyGraph(UPDATE_VMWARE_VSDOMAIN_POLICY_VS), false},
//            {UPDATE_VMWARE_VSPOLICY_WITHOUT_SERVICE_ID_VS,  createVsPolicyWithoutServiceIdGraph(UPDATE_VMWARE_VSPOLICY_WITHOUT_SERVICE_ID_VS), false},
//            {UPDATE_VMWARE_VSPOLICY_NAME_OUT_OF_SYNC_VS,  createVsPolicyNameOutOfSyncGraph(UPDATE_VMWARE_VSPOLICY_NAME_OUT_OF_SYNC_VS), false},
//            {UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_VS,  createOpenstackNoDeploymentSpecGraph(UPDATE_OPENSTACK_NO_DEPLOYMENT_SPEC_VS), false},
//            {UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS,  createOpenstackWithDeploymentSpecGraph(UPDATE_OPENSTACK_DEPLOYMENT_SPEC_VS), false},
//            {UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS,  createOpenstacWhenLockingDeploymentSpecFailsGraph(UPDATE_OPENSTACK_LOCK_DEPLOYMENT_SPEC_FAILS_VS), false},
//            {DELETE_VMWARE_DELETE_SERVICE_INSTANCE_VS,  createDeleteServiceInstanceGraph(DELETE_VMWARE_DELETE_SERVICE_INSTANCE_VS), false},
//            {DELETE_VMWARE_DELETE_SERVICE_MANAGER_VS,  createDeleteServiceManagerGraph(DELETE_VMWARE_DELETE_SERVICE_MANAGER_VS), false},
//            {DELETE_VMWARE_DELETE_SERVICE_VS,  createDeleteServiceGraph(DELETE_VMWARE_DELETE_SERVICE_VS), false},
//            {DELETE_VMWARE_UNDENPLOY_SERVICE_INSTANCE_VS,  createUndenployServiceInstanceGraph(DELETE_VMWARE_UNDENPLOY_SERVICE_INSTANCE_VS), true},
//            {DELETE_OPENSTACK_WITH_DEPLOYMENT_SPECS_VS,  createDeleteOpenStackWithDeploymentSpecGraph(DELETE_OPENSTACK_WITH_DEPLOYMENT_SPECS_VS), false},
//            {DELETE_OPENSTACK_WITH_OS_IMAGE_REF_VS,  createDeleteOpenStackWithOSImageRefGraph(DELETE_OPENSTACK_WITH_OS_IMAGE_REF_VS), false},
//            {DELETE_OPENSTACK_WITH_OS_FLAVOR_REF_VS,  createDeleteOpenStackWithOSFlavorRefGraph(DELETE_OPENSTACK_WITH_OS_FLAVOR_REF_VS), false},
        });
    }

//    private void registerServiceManager(String smId, String name, String url, String password) throws Exception {
//        ServiceManagerElement serviceManager = Mockito.mock(ServiceManagerElement.class);
//        Mockito.doReturn(name).when(serviceManager).getName();
//        Mockito.doReturn(url).when(serviceManager).getCallbackUrl();
//        Mockito.doReturn(password).when(serviceManager).getOscPassword();
//
//        Mockito.doReturn(serviceManager).when(this.serviceManagerApiMock).getServiceManager(smId);
//    }
//
//    private void registerService(String sId, String name, String ip, String password) throws Exception {
//        ServiceElement service = Mockito.mock(ServiceElement.class);
//        Mockito.doReturn(name).when(service).getName();
//        Mockito.doReturn(ip).when(service).getOscIpAddress();
//        Mockito.doReturn(password).when(service).getOscPassword();
//
//        Mockito.doReturn(service).when(this.serviceApiMock).getService(sId);
//    }

    private static String getDefaultServerIp() {
        ServerUtil.setServerIP("127.0.0.1");
        return ServerUtil.getServerIP();
    }
}
