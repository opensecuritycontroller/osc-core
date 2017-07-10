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

import java.util.UUID;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.FailurePolicyType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.OsProtectionEntity;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;

public class CheckPortGroupHookMetaTaskTestData {
    public static DistributedApplianceInstance DAI_PROTECTING_PORT = createDAI("DAI_PROTECTING_PORT");

    public static DistributedApplianceInstance DAI_PROTECTING_PORT_EXISTING_HOOK = createDAI("DAI_PROTECTING_PORT_EXISTING_HOOK");

    public static DistributedApplianceInstance DAI_DELETION_PROTECTING_PORT_EXISTING_HOOK = createDAI("DAI_DELETION_PROTECTING_PORT_EXISTING_HOOK");

    public static DistributedApplianceInstance DAI_DEPLOYED = createDAI("DAI_DEPLOYED");

    public static SecurityGroupInterface SGI_WITHOUT_NET_ELEMENT_WITH_ASSIGNED_DAI =
            createSGIWithDAI(
                    "SGI_WITHOUT_NET_ELEMENT_WITH_ASSIGNED_DAI",
                    DAI_PROTECTING_PORT.getVirtualSystem(),
                    DAI_PROTECTING_PORT,
                    "1");

    public static SecurityGroupInterface SGI_WITH_INSPECTION_HOOK_WITHOUT_ASSIGNED_DAI =
            createSGIWithNetElementId(
                    "SGI_WITH_INSPECTION_HOOK_WITHOUT_ASSIGNED_DAI",
                    null,
                    UUID.randomUUID().toString(),
                    "2",
                    null);

    public static SecurityGroupInterface SGI_DELETED_WITH_INSPECTION_HOOK_WITHOUT_ASSIGNED_DAI =
            createSGIWithNetElementId(
                    "SGI_DELETED_WITH_INSPECTION_HOOK_WITHOUT_ASSIGNED_DAI",
                    null,
                    UUID.randomUUID().toString(),
                    "3",
                    null);

    public static SecurityGroupInterface SGI_WITH_INSPECTION_HOOK_WITH_ASSIGNED_DAI =
            createSGIWithNetElementId(
                    "SGI_WITH_INSPECTION_HOOK_WITH_ASSIGNED_DAI",
                    DAI_PROTECTING_PORT_EXISTING_HOOK.getVirtualSystem(),
                    UUID.randomUUID().toString(),
                    "4",
                    DAI_PROTECTING_PORT_EXISTING_HOOK);

    public static SecurityGroupInterface SGI_DELETED_WITH_INSPECTION_HOOK_WITH_ASSIGNED_DAI =
            createSGIWithNetElementId(
                    "SGI_DELETED_WITH_INSPECTION_HOOK_WITH_ASSIGNED_DAI",
                    DAI_DELETION_PROTECTING_PORT_EXISTING_HOOK.getVirtualSystem(),
                    UUID.randomUUID().toString(),
                    "5",
                    DAI_DELETION_PROTECTING_PORT_EXISTING_HOOK,
                    true);

    public static SecurityGroupInterface SGI_WITHOUT_ASSIGNED_DAI_DOMAIN_NOT_FOUND =
            createSGIWithDAI(
                    "SGI_WITHOUT_ASSIGNED_DAI_DOMAIN_NOT_FOUND",
                    null,
                    null,
                    "6");

    public static SecurityGroupInterface SGI_WITHOUT_ASSIGNED_DAI_FOUND_DEPLOYED_DAI = createSGIWithDAI(
            "SGI_WITHOUT_ASSIGNED_DAI_FOUND_DEPLOYED_DAI",
            DAI_DEPLOYED.getVirtualSystem(),
            null,
            "7");

    public static TaskGraph createInspectionHookGraph(SecurityGroupInterface sgi, DistributedApplianceInstance dai) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new AllocateDAIWithSGIMembersTask().create(sgi, dai));
        expectedGraph.appendTask(new CreatePortGroupHookTask().create(sgi, dai));
        return expectedGraph;
    }

    public static TaskGraph removeInspectionHookDeallocateDAIGraph(SecurityGroupInterface sgi, DistributedApplianceInstance dai) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new RemovePortGroupHookTask().create(sgi));
        expectedGraph.appendTask(new DeallocateDAIOfSGIMembersTask().create(sgi, dai));
        return expectedGraph;
    }

    public static TaskGraph removeInspectionHookGraph(SecurityGroupInterface sgi) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new RemovePortGroupHookTask().create(sgi));
        return expectedGraph;
    }

    public static TaskGraph createEmptyGraph() {
        return new TaskGraph();
    }

    private static SecurityGroupInterface createSGIWithNetElementId(String name, VirtualSystem vs, String netElementId, String tag, DistributedApplianceInstance dai) {
        return createSGIWithNetElementId(name, vs, netElementId, tag, dai, false);
    }

    private static SecurityGroupInterface createSGIWithNetElementId(String name, VirtualSystem vs, String netElementId, String tag, DistributedApplianceInstance dai, boolean deleted) {
        SecurityGroupInterface sgi = createSGIWithDAI(name, vs, dai, tag);
        sgi.setNetworkElementId(netElementId);
        sgi.setMarkedForDeletion(deleted);
        return sgi;
    }

    private static SecurityGroupInterface createSGIWithDAI(String name, VirtualSystem vs, DistributedApplianceInstance dai, String tag) {
        if (vs == null) {
            vs = createVS(name);
        }

        SecurityGroupInterface sgi = new SecurityGroupInterface(vs, null, tag, FailurePolicyType.FAIL_CLOSE, 0L);
        sgi.setName(name + "_SGI");

        SecurityGroup sg = new SecurityGroup(vs.getVirtualizationConnector(), UUID.randomUUID().toString(), name + "_tenant");
        sg.setName(name + "_SG");

        newSGMWithPort(sg, dai, VM.class);

        sgi.addSecurityGroup(sg);
        sg.addSecurityGroupInterface(sgi);

        return sgi;
    }

    private static SecurityGroupMember newSGMWithPort(SecurityGroup sg, DistributedApplianceInstance dai, Class<? extends OsProtectionEntity> entityType) {
        VMPort port = null;
        OsProtectionEntity protectionEntity;

        if (entityType == VM.class) {
            protectionEntity = new VM("region", UUID.randomUUID().toString(), "name");
            port = newVMPort((VM) protectionEntity);
        } else if (entityType == Network.class) {
            protectionEntity = new Network("region", UUID.randomUUID().toString(), "name");
            port = new VMPort((Network) protectionEntity, "mac-address", UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(), null);
        } else {
            protectionEntity = new Subnet("network", UUID.randomUUID().toString(), "name", "region", false);
            port = new VMPort((Subnet) protectionEntity, "mac-address", UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(), null);
        }

        if (dai != null) {
            port.addDai(dai);
            dai.addProtectedPort(port);
        }

        return newSGM(sg, protectionEntity);
    }

    private static SecurityGroupMember newSGM(SecurityGroup sg, OsProtectionEntity protectionEntity) {
        SecurityGroupMember sgm = new SecurityGroupMember(sg, protectionEntity);
        return sgm;
    }

    private static VMPort newVMPort(VM vm) {
        return new VMPort(vm, "mac-address", UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
    }

    private static VirtualSystem createVS(String baseName) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName(baseName + "_vc");
        vc.setVirtualizationType(VirtualizationType.OPENSTACK);
        vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        vc.setProviderIpAddress(baseName + "_providerIp");
        vc.setProviderUsername("Natasha");
        vc.setProviderPassword("********");

        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setIpAddress(baseName + "_mcIp");
        mc.setName(baseName + "_mc");
        mc.setServiceType("foobar");
        mc.setManagerType("buzz");

        Domain domain = new Domain(mc);
        domain.setName(baseName + "_domain");

        Appliance app = new Appliance();
        app.setManagerSoftwareVersion("fizz");
        app.setManagerType("buzz");
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

    private static DistributedApplianceInstance createDAI(String baseName) {
        VirtualSystem vs = createVS(baseName);
        DeploymentSpec ds = new DeploymentSpec(vs, "RegionOne", baseName + "_tenantId", baseName + "_mnId",
                baseName + "_inId", null);
        ds.setName(baseName + "_DS");
        ds.setProjectName(baseName + "_tenantName");
        ds.setManagementNetworkName(baseName + "_mnName");
        ds.setInspectionNetworkName(baseName + "_inName");

        DistributedApplianceInstance dai = new DistributedApplianceInstance(vs);
        dai.setName(baseName + "_DAI");

        dai.setDeploymentSpec(ds);

        return dai;
    }
}
