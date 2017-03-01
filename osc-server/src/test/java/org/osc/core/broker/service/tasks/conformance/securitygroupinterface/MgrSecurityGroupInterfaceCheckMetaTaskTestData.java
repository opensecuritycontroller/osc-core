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
package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mockito.Mockito;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.sdk.manager.element.ManagerSecurityGroupInterfaceElement;

public class MgrSecurityGroupInterfaceCheckMetaTaskTestData {

    public static List<VirtualSystem> TEST_VIRTUAL_SYSTEMS = new ArrayList<>();
    public static List<DistributedAppliance> TEST_DISTRIBUTED_APPLIANCE = new ArrayList<>();

    public static VirtualizationConnector CREATE_VC = createVC("vcName");

    //MGR SGI's
    public static ManagerSecurityGroupInterfaceElement MGR_SGI = createManagerSecurityGroupInterface("MGR_SGI_ID", "MGR_SGI_NAME", "POLICY_ID", "TAG");
    public static ManagerSecurityGroupInterfaceElement MGR_SGI_AND_SGI_WITH_SAME_NAME = createManagerSecurityGroupInterface("MGR_SGI_ID", "MATCHING_NAME", "POLICY_ID_1", "TAG_1");
    public static ManagerSecurityGroupInterfaceElement MGR_SGI_TO_BE_DELETED = createManagerSecurityGroupInterface("MGR_SGI_ID", "MGR_SGI_NAME", null, null);

    //SGI's
    public static SecurityGroupInterface SGI_NOT_MARK_FOR_DELETION_WITH_ID_VS_SGI = createSgi("SGI_ID", "SGI_NAME", false, "POLICY_ID", "TAG");
    public static SecurityGroupInterface SGI_NOT_MARK_FOR_DELETION_WITH_DIFFERENT_POLICY_ID_AND_TAG_VS_SGI = createSgi("MGR_SGI_ID", "MATCHING_NAME", false, "POLICY_ID", "TAG");
    public static SecurityGroupInterface SGI_MARK_FOR_DELETION_VS_SGI = createSgi(null, "MATCHING_NAME", true, null, null);

    public static SecurityGroupInterface SGI_NOT_MARK_FOR_DELETION_WITH_ID_DA_SGI = createSgi("SGI_ID", "SGI_NAME", false, "POLICY_ID", "TAG");
    public static SecurityGroupInterface SGI_NOT_MARK_FOR_DELETION_WITH_DIFFERENT_POLICY_ID_AND_TAG_DA_SGI = createSgi("MGR_SGI_ID", "MATCHING_NAME", false, "POLICY_ID", "TAG");
    public static SecurityGroupInterface SGI_MARK_FOR_DELETION_DA_SGI = createSgi(null, "MATCHING_NAME", true, null, null);

    //List of MGR SGI's
    public static List<ManagerSecurityGroupInterfaceElement> MGR_SGI_AND_MGR_SGI_TO_BE_DELETED_LIST = createManagerSecurityGroupInterfaceList(MGR_SGI, MGR_SGI_TO_BE_DELETED);
    public static List<ManagerSecurityGroupInterfaceElement> MGR_SGI_AND_MGR_SGI_AND_SGI_WITH_SAME_NAME_LIST = createManagerSecurityGroupInterfaceList(MGR_SGI_TO_BE_DELETED, MGR_SGI_AND_SGI_WITH_SAME_NAME);

    //VS's
    private static VirtualSystem VS_1 = createBaseVS(101L, "vsName", CREATE_VC, createMC(101L, "MC_01"));
    public static VirtualSystem VS_WITH_SGI_AND_NO_MANAGER_SGI_VS = CreateVSWithSgi(VS_1,SGI_NOT_MARK_FOR_DELETION_WITH_ID_VS_SGI);

    private static VirtualSystem VS_2 = createBaseVS(102L, "vsName", CREATE_VC, createMC(102L, "MC_02"));
    public static VirtualSystem VS_WITH_MANAGER_SGI_VS = CreateVSWithSgi(VS_2, SGI_NOT_MARK_FOR_DELETION_WITH_DIFFERENT_POLICY_ID_AND_TAG_VS_SGI);

    private static VirtualSystem VS_3 = createBaseVS(103L, "vsName", CREATE_VC, createMC(103L, "MC_03"));
    public static VirtualSystem VS_WITH_MANAGER_SGI_TO_BE_DELETED_VS = CreateVSWithSgi(VS_3, SGI_MARK_FOR_DELETION_VS_SGI);

    //VS with multiple SGI's
    private static VirtualSystem VS_4 = createBaseVS(104L, "vsName", CREATE_VC, createMC(104L, "MC_04"));
    public static VirtualSystem VS_WITH_SGI_NOT_MARK_FOR_DELETION_WITH_ID_VS_SGI_AND_SGI_MARK_FOR_DELETION_VS_SGI = CreateVSWithSgi(VS_4, SGI_NOT_MARK_FOR_DELETION_WITH_ID_VS_SGI, SGI_MARK_FOR_DELETION_VS_SGI);

    //DA's
    private static DistributedAppliance DA_1 = createBaseDA(1L, 105L, "vsName", CREATE_VC, createMC(5L, "MC_5"));
    public static DistributedAppliance DA_WITH_SGI_AND_NO_MANAGER_SGI_DA = CreateDAWithSgi(DA_1, SGI_NOT_MARK_FOR_DELETION_WITH_ID_DA_SGI);

    private static DistributedAppliance DA_2 = createBaseDA(2L, 106L, "vsName", CREATE_VC, createMC(6L, "MC_6"));
    public static DistributedAppliance DA_WITH_MANAGER_SGI_DA = CreateDAWithSgi(DA_2, SGI_NOT_MARK_FOR_DELETION_WITH_DIFFERENT_POLICY_ID_AND_TAG_DA_SGI);

    private static DistributedAppliance DA_3 = createBaseDA(3L, 107L, "vsName", CREATE_VC, createMC(7L, "MC_7"));
    public static DistributedAppliance DA_WITH_MANAGER_SGI_TO_BE_DELETED_DA = CreateDAWithSgi(DA_3, SGI_MARK_FOR_DELETION_DA_SGI);

    private static VirtualizationConnector createVC(String vcName) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName(vcName);
        return vc;
    }

    private static ApplianceManagerConnector createMC(Long mcId, String mcName) {
        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setId(mcId);
        mc.setName(mcName);
        return mc;
    }

    private static ManagerSecurityGroupInterfaceElement createManagerSecurityGroupInterface(String id, String name,
            String policyId, String tag) {
        ManagerSecurityGroupInterfaceElement managerSecurityGroupInterface = Mockito
                .mock(ManagerSecurityGroupInterfaceElement.class);
        Mockito.doReturn(name).when(managerSecurityGroupInterface).getName();
        Mockito.doReturn(id).when(managerSecurityGroupInterface).getSecurityGroupInterfaceId();
        Mockito.doReturn(policyId).when(managerSecurityGroupInterface).getPolicyId();
        Mockito.doReturn(tag).when(managerSecurityGroupInterface).getTag();
        return managerSecurityGroupInterface;
    }

    private static List<ManagerSecurityGroupInterfaceElement> createManagerSecurityGroupInterfaceList(
            ManagerSecurityGroupInterfaceElement... elements) {
        List<ManagerSecurityGroupInterfaceElement> mgrSgiElem = new ArrayList<>();
        for (ManagerSecurityGroupInterfaceElement iter : elements) {
            mgrSgiElem.add(iter);
        }
        return mgrSgiElem;
    }

    private static SecurityGroupInterface createSgi(String sgiId, String sgiName, boolean isMarkedForDeletion,
            String tag, String policyId) {
        SecurityGroupInterface sgi = Mockito.mock(SecurityGroupInterface.class);
        Mockito.when(sgi.getMgrSecurityGroupIntefaceId()).thenReturn(sgiId);
        Mockito.when(sgi.getName()).thenReturn(sgiName);
        Mockito.when(sgi.getMarkedForDeletion()).thenReturn(isMarkedForDeletion);
        Mockito.when(sgi.getTag()).thenReturn(tag);
        Policy policy = Mockito.mock(Policy.class);
        Mockito.when(sgi.getMgrPolicy()).thenReturn(policy);
        Mockito.when(policy.getMgrPolicyId()).thenReturn(policyId);

        return sgi;

    }

    private static VirtualSystem createBaseVS(Long vsId, String vsName, VirtualizationConnector vc,
            ApplianceManagerConnector mc) {
        VirtualSystem vs = Mockito.mock(VirtualSystem.class);
        Mockito.when(vs.getId()).thenReturn(vsId);
        Mockito.when(vs.getName()).thenReturn(vsName);
        Mockito.when(vs.getVirtualizationConnector()).thenReturn(vc);
        DistributedAppliance da = Mockito.mock(DistributedAppliance.class);
        Mockito.when(vs.getDistributedAppliance()).thenReturn(da);
        Mockito.when(da.getApplianceManagerConnector()).thenReturn(mc);

        return vs;
    }

    private static VirtualSystem CreateVSWithSgi(VirtualSystem vs, SecurityGroupInterface... interfaces) {
        Set<SecurityGroupInterface> set = new HashSet<>();
        for (SecurityGroupInterface sgi : interfaces) {
            set.add(sgi);
            Mockito.when(sgi.getVirtualSystem()).thenReturn(vs);
        }
        Mockito.when(vs.getSecurityGroupInterfaces()).thenReturn(set);

        TEST_VIRTUAL_SYSTEMS.add(vs);
        return vs;
    }

    private static DistributedAppliance createBaseDA(Long daId, Long vsId, String vsName, VirtualizationConnector vc,
            ApplianceManagerConnector mc) {
        VirtualSystem vs = Mockito.mock(VirtualSystem.class);
        Mockito.when(vs.getId()).thenReturn(vsId);
        Mockito.when(vs.getName()).thenReturn(vsName);
        Mockito.when(vs.getVirtualizationConnector()).thenReturn(vc);
        Set<VirtualSystem> vsSet = new HashSet<>();
        vsSet.add(vs);
        DistributedAppliance da = Mockito.mock(DistributedAppliance.class);
        Mockito.when(da.getVirtualSystems()).thenReturn(vsSet);
        Mockito.when(da.getId()).thenReturn(daId);
        Mockito.when(vs.getDistributedAppliance()).thenReturn(da);
        Mockito.when(da.getApplianceManagerConnector()).thenReturn(mc);

        return da;
    }

    private static DistributedAppliance CreateDAWithSgi(DistributedAppliance da, SecurityGroupInterface... interfaces) {
        Set<SecurityGroupInterface> set = new HashSet<>();
        for (VirtualSystem vs : da.getVirtualSystems()) {
            for (SecurityGroupInterface sgi : interfaces) {
                set.add(sgi);
                Mockito.when(sgi.getVirtualSystem()).thenReturn(vs);
            }
            Mockito.when(vs.getSecurityGroupInterfaces()).thenReturn(set);
        }

        TEST_DISTRIBUTED_APPLIANCE.add(da);
        return da;
    }

    private static Task createNewUnlockObjectTask(VirtualSystem vs) {
        return new UnlockObjectTask(
                new LockObjectReference(vs.getDistributedAppliance().getApplianceManagerConnector()),
                LockType.WRITE_LOCK);
    }

    public static TaskGraph createAndDeleteStaleMgrSecurityGroupInterfaceGraph(VirtualSystem vs,
            SecurityGroupInterface sgi, ManagerSecurityGroupInterfaceElement mgrSgi) {
        TaskGraph expectedGraph = new TaskGraph();

        TaskGraph tg = new TaskGraph();
        tg.appendTask(new CreateMgrSecurityGroupInterfaceTask(sgi));
        tg.appendTask(new DeleteMgrSecurityGroupInterfaceTask(vs, mgrSgi));

        expectedGraph.addTaskGraph(tg);
        expectedGraph.appendTask(createNewUnlockObjectTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    public static TaskGraph updateMgrSecurityGroupInterfaceGraph(VirtualSystem vs, SecurityGroupInterface sgi) {
        TaskGraph expectedGraph = new TaskGraph();

        TaskGraph tg = new TaskGraph();
        tg.appendTask(new UpdateMgrSecurityGroupInterfaceTask(sgi));

        expectedGraph.addTaskGraph(tg);
        expectedGraph.appendTask(createNewUnlockObjectTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    public static TaskGraph deleteMgrSecurityGroupGraph(VirtualSystem vs, ManagerSecurityGroupInterfaceElement mgrSgi) {
        TaskGraph expectedGraph = new TaskGraph();

        TaskGraph tg = new TaskGraph();
        tg.appendTask(new DeleteMgrSecurityGroupInterfaceTask(vs, mgrSgi));

        expectedGraph.addTaskGraph(tg);
        expectedGraph.appendTask(createNewUnlockObjectTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    public static TaskGraph vsWithMultipleSgiGraph(VirtualSystem vs, SecurityGroupInterface sgi) {
        TaskGraph expectedGraph = new TaskGraph();

        TaskGraph tg = new TaskGraph();
        tg.appendTask(new CreateMgrSecurityGroupInterfaceTask(sgi));
        tg.appendTask(new DeleteMgrSecurityGroupInterfaceTask(vs, MGR_SGI));
        tg.appendTask(new DeleteMgrSecurityGroupInterfaceTask(vs, MGR_SGI_TO_BE_DELETED));

        expectedGraph.addTaskGraph(tg);
        expectedGraph.appendTask(createNewUnlockObjectTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

}
