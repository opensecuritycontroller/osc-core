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

import static org.osc.core.common.virtualization.VirtualizationType.KUBERNETES;
import static org.osc.core.common.virtualization.VirtualizationType.OPENSTACK;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.FailurePolicyType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.tasks.conformance.k8s.securitygroup.UpdateOrDeleteK8sSecurityGroupMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.MgrSecurityGroupInterfacesCheckMetaTask;
import org.osc.core.common.job.TaskGuard;
import org.osc.core.common.virtualization.VirtualizationType;

class SecurityGroupCheckMetaTaskTestData {
    public final static String POLICY_MAPPING_SUPPORTED_MGR_TYPE = "NSM";
    public final static String POLICY_MAPPING_NOT_SUPPORTED_MGR_TYPE = "SMC";

    public final static String NO_MC_POLICY_MAPPING_SUPPORTED = "NO_MC_POLICY_MAPPING_SUPPORTED";
    public final static String NO_MC_POLICY_MAPPING_SUPPORTED_K8S = "NO_MC_POLICY_MAPPING_SUPPORTED_K8S";
    public final static String SINGLE_MC_POLICY_MAPPING_SUPPORTED = "SINGLE_MC_POLICY_MAPPING_SUPPORTED";
    public final static String MULTIPLE_MC_POLICY_MAPPING_SUPPORTED = "MULTIPLE_MC_POLICY_MAPPING_SUPPORTED";

    public VirtualSystem MC_POLICY_MAPPING_NOT_SUPPORTED_VS = createVirtualSystem(NO_MC_POLICY_MAPPING_SUPPORTED, POLICY_MAPPING_NOT_SUPPORTED_MGR_TYPE, OPENSTACK);
    public VirtualSystem MC_POLICY_MAPPING_SUPPORTED_VS = createVirtualSystem(SINGLE_MC_POLICY_MAPPING_SUPPORTED, POLICY_MAPPING_SUPPORTED_MGR_TYPE, OPENSTACK);
    public VirtualSystem MC_POLICY_MAPPING_SUPPORTED_VS_2 = createVirtualSystem(MULTIPLE_MC_POLICY_MAPPING_SUPPORTED, POLICY_MAPPING_SUPPORTED_MGR_TYPE, OPENSTACK);
    public VirtualSystem MC_POLICY_MAPPING_NOT_SUPPORTED_K8S_VS = createVirtualSystem(NO_MC_POLICY_MAPPING_SUPPORTED_K8S, POLICY_MAPPING_NOT_SUPPORTED_MGR_TYPE, KUBERNETES);

    public List<VirtualSystem> MC_POLICY_MAPPING_SUPPORTED_VS_LIST = Arrays.asList(this.MC_POLICY_MAPPING_SUPPORTED_VS, this.MC_POLICY_MAPPING_SUPPORTED_VS_2);

    public List<SecurityGroup> TEST_SECURITY_GROUPS = new ArrayList<>();

    public SecurityGroup NO_MC_POLICY_MAPPING_SUPPORTED_SG = createSecurityGroup(NO_MC_POLICY_MAPPING_SUPPORTED,
            this.MC_POLICY_MAPPING_NOT_SUPPORTED_VS);
    public SecurityGroup SINGLE_MC_POLICY_MAPPING_SUPPORTED_SG = createSecurityGroup(SINGLE_MC_POLICY_MAPPING_SUPPORTED,
            this.MC_POLICY_MAPPING_SUPPORTED_VS);
    public SecurityGroup NO_MC_POLICY_MAPPING_SUPPORTED_K8S_SG = createSecurityGroup(NO_MC_POLICY_MAPPING_SUPPORTED_K8S,
            this.MC_POLICY_MAPPING_NOT_SUPPORTED_K8S_VS);

    public TaskGraph createNoMcPolicyMappingGraph(SecurityGroup sg) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new ValidateSecurityGroupProjectTask().create(sg));
        expectedGraph.appendTask(new SecurityGroupUpdateOrDeleteMetaTask().create(sg));
        return expectedGraph;
    }

    public TaskGraph createSingleMcPolicyMappingGraph(SecurityGroup sg) {
        TaskGraph expectedGraph = createNoMcPolicyMappingGraph(sg);
        expectedGraph.appendTask(new MgrSecurityGroupInterfacesCheckMetaTask().create(this.MC_POLICY_MAPPING_SUPPORTED_VS),
                TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    public TaskGraph createMultipleMcPolicyMappingGraph(SecurityGroup sg) {
        TaskGraph expectedGraph = createNoMcPolicyMappingGraph(sg);
        for (VirtualSystem vs : this.MC_POLICY_MAPPING_SUPPORTED_VS_LIST) {
            expectedGraph.appendTask(new MgrSecurityGroupInterfacesCheckMetaTask().create(vs),
                    TaskGuard.ALL_PREDECESSORS_COMPLETED);
        }

        return expectedGraph;
    }

    public TaskGraph createNoMcPolicyMappingK8sGraph(SecurityGroup sg) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTask(new UpdateOrDeleteK8sSecurityGroupMetaTask().create(sg));
        return expectedGraph;
    }


    private SecurityGroup createSecurityGroup(String baseName,
            VirtualSystem vs) {

        Policy policy = new Policy(
                vs.getDistributedAppliance().getApplianceManagerConnector(),
                vs.getDomain());
        policy.setName(baseName + "_policy");
        policy.setMgrPolicyId(baseName + "_mgrPolicy");
        Set<Policy> policySet = new HashSet<>();
        policySet.add(policy);

        SecurityGroupInterface sgi = new SecurityGroupInterface(vs, policySet, baseName + "_tag", FailurePolicyType.NA, 2L);
        sgi.setName(baseName + "_sgi");

        SecurityGroup sg = new SecurityGroup(vs.getVirtualizationConnector(), null, null);
        sg.setName(baseName + "_sg");
        sg.addSecurityGroupInterface(sgi);
        sgi.setSecurityGroup(sg);

        this.TEST_SECURITY_GROUPS.add(sg);
        return sg;
    }

    private VirtualSystem createVirtualSystem(String baseName,
            String mgrType, VirtualizationType virtualizationType) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName(baseName + "_vc");
        vc.setVirtualizationType(virtualizationType);
        vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        vc.setProviderIpAddress(baseName + "_providerIp");
        vc.setProviderUsername("Natasha");
        vc.setProviderPassword("********");

        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setIpAddress(baseName + "_mcIp");
        mc.setName(baseName + "_mc");
        mc.setServiceType("foobar");
        mc.setManagerType(mgrType.toString());

        Domain domain = new Domain(mc);
        domain.setName(baseName + "_domain");

        Appliance app = new Appliance();
        app.setManagerSoftwareVersion("fizz");
        app.setManagerType(mgrType);
        app.setModel(baseName + "_model");

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion(app);
        asv.setApplianceSoftwareVersion("softwareVersion");
        asv.setImageUrl(baseName + "_image");
        asv.setVirtualizarionSoftwareVersion(vc.getVirtualizationSoftwareVersion());
        asv.setVirtualizationType(vc.getVirtualizationType());

        DistributedAppliance da = new DistributedAppliance(mc);
        da.setName(baseName + "_da");
        da.setApplianceVersion("foo");
        da.setAppliance(app);

        VirtualSystem vs = new VirtualSystem(da);
        vs.setApplianceSoftwareVersion(asv);
        vs.setDomain(domain);
        vs.setVirtualizationConnector(vc);
        vs.setMarkedForDeletion(false);
        vs.setName(baseName + "_vs");
        vs.setMgrId(baseName + "_mgrId");
        return vs;
    }
}