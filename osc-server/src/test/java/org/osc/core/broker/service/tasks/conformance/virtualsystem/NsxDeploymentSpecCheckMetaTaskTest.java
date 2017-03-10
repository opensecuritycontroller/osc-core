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

import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.IMAGE_URL_OUT_OF_SYNC;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.TEST_VIRTUAL_SYSTEMS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_UPDATE_NSX_SCHED_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.VMWARE_NEW_DIST_APPL_NO_DEPLOYMENT_SPEC_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.VMWARE_NSX_ALL_DEPLOY_SPEC_MISSING_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.VMWARE_NSX_DS_OUT_OF_SYNC_5_5_X_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.VMWARE_NSX_DS_OUT_OF_SYNC_6_X_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_NSX_OOS_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_VS;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.createApplVersionImageUrlOutOfSyncUpdateNsxSchedGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.createApplianceVersionImageUrlOutOfSyncGraph;
import static org.osc.core.broker.service.tasks.conformance.virtualsystem.NsxDeploymentSpecCheckMetaTaskTestData.createRegisterDeploySpecExpectedGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VmwareSoftwareVersion;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.VersionedDeploymentSpec;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.sdk.sdn.api.DeploymentSpecApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({VMwareSdnApiFactory.class, RegisterDeploymentSpecTask.class})
public class NsxDeploymentSpecCheckMetaTaskTest {
    private static final String VMWARE_5_5_STRING = "5.5";

    private static final String VMWARE_6_STRING = "6";

    @Mock
    public Session sessionMock;

    private static String DEFAULT_IMAGE_URL = "DEFAULT_IMAGE_URL";
    private static String ALL_DEPLOY_SPECS_MISSING = "ALL";

    private VirtualSystem vs;
    private DeploymentSpecApi deploymentSpecApiMock;

    private TaskGraph expectedGraph;

    private boolean updateNsxServiceAttributesScheduled;

    public NsxDeploymentSpecCheckMetaTaskTest(VirtualSystem vs, TaskGraph tg,
            boolean updateNsxServiceAttributesScheduled) {
        this.vs = vs;
        this.expectedGraph = tg;
        this.updateNsxServiceAttributesScheduled = updateNsxServiceAttributesScheduled;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
        this.deploymentSpecApiMock = Mockito.mock(DeploymentSpecApi.class);

        PowerMockito.mockStatic(VMwareSdnApiFactory.class);

        for (VirtualSystem vs: TEST_VIRTUAL_SYSTEMS) {
            Mockito.doReturn(vs).when(this.sessionMock).get(VirtualSystem.class, vs.getId());
        }

        stubRegisterDeploySpecsForANewDistributedAppliance(VMWARE_NEW_DIST_APPL_NO_DEPLOYMENT_SPEC_VS);//0
        registerVersionedDeploymentSpec(IMAGE_URL_OUT_OF_SYNC, VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_VS, null); //1
        registerVersionedDeploymentSpec(IMAGE_URL_OUT_OF_SYNC,
                VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_UPDATE_NSX_SCHED_VS, null);//2
        registerVersionedDeploymentSpecDbUpgrade(VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_VS, false); //3
        registerVersionedDeploymentSpecDbUpgrade(VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_NSX_OOS_VS, true);  //4
        registerVersionedDeploymentSpec(null, VMWARE_NSX_DS_OUT_OF_SYNC_6_X_VS,
                VMWARE_6_STRING);//5
        registerVersionedDeploymentSpec(null, VMWARE_NSX_DS_OUT_OF_SYNC_5_5_X_VS,
                VMWARE_5_5_STRING); //6
        registerVersionedDeploymentSpec(null, VMWARE_NSX_ALL_DEPLOY_SPEC_MISSING_VS,
                ALL_DEPLOY_SPECS_MISSING); //7

        PowerMockito.spy(RegisterDeploymentSpecTask.class);
        PowerMockito.doReturn(DEFAULT_IMAGE_URL).when(RegisterDeploymentSpecTask.class, "generateOvfUrl", Mockito.any(String.class));

    }

    @Test
    public void testExecuteTransaction_WithVariousVirtualSystems_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        NsxDeploymentSpecCheckMetaTask task = new NsxDeploymentSpecCheckMetaTask(this.vs, this.updateNsxServiceAttributesScheduled);

        // Act.
        task.executeTransaction(this.sessionMock);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    private void stubRegisterDeploySpecsForANewDistributedAppliance(VirtualSystem vs) throws Exception{
        List<VersionedDeploymentSpec> apiVersionedDeplSpecs= java.util.Collections.emptyList();

        Mockito.when(VMwareSdnApiFactory.createDeploymentSpecApi(vs)).thenReturn(this.deploymentSpecApiMock);
        Mockito.doReturn(apiVersionedDeplSpecs).when(this.deploymentSpecApiMock).getDeploymentSpecs(vs.getNsxServiceId());
    }

    private void registerVersionedDeploymentSpec(String imageUrl, VirtualSystem vs, String missingDS) throws Exception {
        VersionedDeploymentSpec vds = new VersionedDeploymentSpec();
        vds.setOvfUrl(imageUrl);
        vds.setHostVersion(VMWARE_5_5_STRING + RegisterDeploymentSpecTask.ALL_MINOR_VERSIONS);
        VersionedDeploymentSpec vds2 = new VersionedDeploymentSpec();
        vds2.setOvfUrl(imageUrl);
        vds2.setHostVersion(VMWARE_6_STRING + RegisterDeploymentSpecTask.ALL_MINOR_VERSIONS);
        List<VersionedDeploymentSpec> versionedSpecsList = new ArrayList<VersionedDeploymentSpec>();
        if (missingDS==null){
            versionedSpecsList.add(vds);
            versionedSpecsList.add(vds2);
        } else if (missingDS.equals(ALL_DEPLOY_SPECS_MISSING)){
            versionedSpecsList = Collections.emptyList();
        } else if (missingDS.equals(VMWARE_5_5_STRING)){
            versionedSpecsList.add(vds2);
        } else if (missingDS.equals(VMWARE_6_STRING)){
            versionedSpecsList.add(vds);
        }

        Mockito.when(VMwareSdnApiFactory.createDeploymentSpecApi(vs)).thenReturn(this.deploymentSpecApiMock);
        Mockito.doReturn(versionedSpecsList).when(this.deploymentSpecApiMock).getDeploymentSpecs(vs.getNsxServiceId());
    }

    private void registerVersionedDeploymentSpecDbUpgrade(VirtualSystem vs, boolean outOfSync) throws Exception {
        VersionedDeploymentSpec vds = new VersionedDeploymentSpec();
        vds.setHostVersion(VMWARE_5_5_STRING + RegisterDeploymentSpecTask.ALL_MINOR_VERSIONS);
        List<VersionedDeploymentSpec> versionedSpecsList = new ArrayList<VersionedDeploymentSpec>();
        versionedSpecsList.add(vds);

        Mockito.when(VMwareSdnApiFactory.createDeploymentSpecApi(vs)).thenReturn(this.deploymentSpecApiMock);
        if (!outOfSync){
            Mockito.doReturn(versionedSpecsList).when(this.deploymentSpecApiMock).getDeploymentSpecs(vs.getNsxServiceId());
        } else {
            Mockito.doReturn(new ArrayList<VersionedDeploymentSpec>()).when(this.deploymentSpecApiMock).getDeploymentSpecs(vs.getNsxServiceId());
        }
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {

            { VMWARE_NEW_DIST_APPL_NO_DEPLOYMENT_SPEC_VS,
                createRegisterDeploySpecExpectedGraph(VMWARE_NEW_DIST_APPL_NO_DEPLOYMENT_SPEC_VS,
                        VmwareSoftwareVersion.VMWARE_V5_5, VmwareSoftwareVersion.VMWARE_V6),
                false }, //1
            { VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_VS,
                    createApplianceVersionImageUrlOutOfSyncGraph(VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_VS),
                    false }, //1
            { VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_UPDATE_NSX_SCHED_VS,
                        createApplVersionImageUrlOutOfSyncUpdateNsxSchedGraph(
                                VMWARE_APPLIANCEVERSION_IMAGE_URL_OUT_OF_SYNC_UPDATE_NSX_SCHED_VS),
                        true }, //2
            { VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_VS,
                            createRegisterDeploySpecExpectedGraph(VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_VS,
                                    VmwareSoftwareVersion.VMWARE_V6),
                            false }, //3
            { VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_NSX_OOS_VS,
                                createRegisterDeploySpecExpectedGraph(VMWARE_OSC_DB_UPGRADE_NEW_ESX_VERSION_SUPPORT_NSX_OOS_VS,
                                        VmwareSoftwareVersion.VMWARE_V5_5, VmwareSoftwareVersion.VMWARE_V6),
                                false }, //4
            { VMWARE_NSX_DS_OUT_OF_SYNC_6_X_VS,
                                    createRegisterDeploySpecExpectedGraph(VMWARE_NSX_DS_OUT_OF_SYNC_6_X_VS, VmwareSoftwareVersion.VMWARE_V6), false }, //5
            { VMWARE_NSX_DS_OUT_OF_SYNC_5_5_X_VS,
                                        createRegisterDeploySpecExpectedGraph(VMWARE_NSX_DS_OUT_OF_SYNC_5_5_X_VS, VmwareSoftwareVersion.VMWARE_V5_5),
                                        false }, //6
            { VMWARE_NSX_ALL_DEPLOY_SPEC_MISSING_VS, createRegisterDeploySpecExpectedGraph(VMWARE_NSX_ALL_DEPLOY_SPEC_MISSING_VS,
                    VmwareSoftwareVersion.VMWARE_V5_5, VmwareSoftwareVersion.VMWARE_V6), false }, //7
        });
    }
}
