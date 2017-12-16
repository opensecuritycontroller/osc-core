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
package org.osc.core.broker.service.tasks.conformance.securitygroup;

import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.mockito.Mockito;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.FailurePolicyType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.model.entities.virtualization.k8s.PodPort;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.sdk.manager.element.ManagerSecurityGroupElement;

public class MgrSecurityGroupCheckMetaTaskTestData {

    public final static String MGR_TYPE = "SMC";

    public static SecurityGroup NON_ORPHAN_SG_SGI_MARKED_FOR_DELETION = createSecurityGroup("NON_ORPHAN_SG_SGI_MARKED_FOR_DELETION");
    public static VirtualSystem NON_ORPHAN_SG_SGI_MARKED_FOR_DELETION_VS =
            NON_ORPHAN_SG_SGI_MARKED_FOR_DELETION.getVirtualizationConnector().getVirtualSystems().iterator().next();

    public static ManagerSecurityGroupElement MGR_SG_ELEMENT = createMgrSecurityGroupElement();

    public static TaskGraph deleteMgrSecGroupGraph(VirtualSystem vs, ManagerSecurityGroupElement mgrSecGroupElement) {
        TaskGraph expectedGraph = new TaskGraph();

        expectedGraph.appendTask(new DeleteMgrSecurityGroupTask().create(vs, mgrSecGroupElement));

        return expectedGraph;
    }

    public static void persistObjects(EntityManager em, SecurityGroup sg) {
        Set<VirtualSystem> virtualSystems = sg.getVirtualizationConnector().getVirtualSystems();

        em.getTransaction().begin();

        em.persist(sg.getVirtualizationConnector());

        for (VirtualSystem vs : virtualSystems) {
            em.persist(vs.getDomain().getApplianceManagerConnector());
            em.persist(vs.getApplianceSoftwareVersion().getAppliance());
            em.persist(vs.getApplianceSoftwareVersion());
            em.persist(vs.getDistributedAppliance());
            em.persist(vs.getDomain());
            em.persist(vs);
        }

        em.persist(sg);
        for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
            if (!sgm.getPodPorts().isEmpty()) {
                for (Pod pod : sgm.getLabel().getPods()) {
                    for (PodPort podPort : pod.getPorts()) {
                        em.persist(podPort);
                    }

                    em.persist(pod);
                }
            }

            em.persist(sgm.getLabel());
            em.persist(sgm);
        }

        sg.getSecurityGroupInterfaces().forEach(sgi -> em.persist(sgi));

        em.getTransaction().commit();
    }

    private static SecurityGroup createSecurityGroup(String baseName) {
        VirtualSystem vs = createVirtualSystem(baseName, MGR_TYPE);

        SecurityGroup sg = new SecurityGroup(vs.getVirtualizationConnector(), null, null);
        sg.setName(baseName + "_sg");

        SecurityGroupInterface sgi = new SecurityGroupInterface(vs, null, "1", FailurePolicyType.FAIL_CLOSE, 1L);
        sgi.setName(baseName + "_SGI");
        sgi.setSecurityGroup(sg);
        sgi.setMarkedForDeletion(true);
        sg.addSecurityGroupInterface(sgi);

        return sg;
    }

    private static  ManagerSecurityGroupElement createMgrSecurityGroupElement() {
        ManagerSecurityGroupElement mgrSecGroupElement = Mockito.mock(ManagerSecurityGroupElement.class);
        Mockito.when(mgrSecGroupElement.getSGId()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(mgrSecGroupElement.getName()).thenReturn(UUID.randomUUID().toString());

        return mgrSecGroupElement;
    }

    private static VirtualSystem createVirtualSystem(String baseName, String mgrType) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName(baseName + "_vc");
        vc.setVirtualizationType(VirtualizationType.KUBERNETES);
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
        vc.setAdminDomainId(domain.getName());

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

        vc.getVirtualSystems().add(vs);
        return vs;
    }
}
