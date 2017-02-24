/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupUpdateOrDeleteMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.ValidateSecurityGroupTenantTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.MgrSecurityGroupInterfacesCheckMetaTask;

class SecurityGroupCheckMetaTaskTestData {
    public static List<SecurityGroup> TEST_SECURITY_GROUPS = new ArrayList<SecurityGroup>();

    public static SecurityGroup NO_MC_POLICY_MAPPING_SUPPORTED_SG = createSecurityGroup(1L);
    public static SecurityGroup SINGLE_MC_POLICY_MAPPING_SUPPORTED_SG = createSecurityGroup(2L);
    public static SecurityGroup MULTIPLE_MC_POLICY_MAPPING_SUPPORTED_SG = createSecurityGroup(3L);

    public static VirtualSystem MC_POLICY_MAPPING_NOT_SUPPORTED_VS = createVirtualSystem(101L);
    public static VirtualSystem MC_POLICY_MAPPING_SUPPORTED_VS = createVirtualSystem(102L);
    public static List<VirtualSystem> MC_POLICY_MAPPING_SUPPORTED_VS_LIST =
            Arrays.asList(MC_POLICY_MAPPING_SUPPORTED_VS, createVirtualSystem(103L));

    public static TaskGraph createNoMcPolicyMappingGraph(SecurityGroup sg) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new ValidateSecurityGroupTenantTask(sg));
        expectedGraph.appendTask(new SecurityGroupUpdateOrDeleteMetaTask(sg));
        return expectedGraph;
    }

    public static TaskGraph createSingleMcPolicyMappingGraph(SecurityGroup sg) {
        TaskGraph expectedGraph = createNoMcPolicyMappingGraph(sg);
        expectedGraph.appendTask(new MgrSecurityGroupInterfacesCheckMetaTask(MC_POLICY_MAPPING_SUPPORTED_VS),
                TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    public static TaskGraph createMultipleMcPolicyMappingGraph(SecurityGroup sg) {
        TaskGraph expectedGraph = createNoMcPolicyMappingGraph(sg);
        for (VirtualSystem vs : MC_POLICY_MAPPING_SUPPORTED_VS_LIST) {
            expectedGraph.appendTask(new MgrSecurityGroupInterfacesCheckMetaTask(vs),
                    TaskGuard.ALL_PREDECESSORS_COMPLETED);
        }

        return expectedGraph;
    }

    private static SecurityGroup createSecurityGroup(Long sgId) {
        SecurityGroup sg = new SecurityGroup(null, null);
        sg.setId(sgId);
        sg.addSecurityGroupInterface(new SecurityGroupInterface());

        TEST_SECURITY_GROUPS.add(sg);
        return sg;
    }

    private static VirtualSystem createVirtualSystem(Long vsId) {
        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setName("MC_NAME");
        DistributedAppliance da = new DistributedAppliance(mc);
        da.setName("DA_NAME");
        VirtualSystem vs = new VirtualSystem(da);
        vs.setId(vsId);
        vs.setMgrId("MGR_ID");
        return vs;
    }
}
