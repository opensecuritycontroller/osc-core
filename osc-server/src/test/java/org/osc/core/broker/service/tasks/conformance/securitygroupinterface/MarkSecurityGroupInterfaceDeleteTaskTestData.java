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

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.FailurePolicyType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.common.virtualization.VirtualizationType;

public class MarkSecurityGroupInterfaceDeleteTaskTestData {
    public static final SecurityGroupInterface NOT_YET_MARKED_FOR_DELETE_SGI = createSGI("NOT_YET_MARKED_FOR_DELETE_SGI", false);
    public static final SecurityGroupInterface ALREADY_MARKED_FOR_DELETE_SGI = createSGI("ALREADY_MARKED_FOR_DELETE_SGI", true);

    private static final String MGR_TYPE = "SMC";

    private static SecurityGroupInterface createSGI(String baseName, boolean isDelete) {
        VirtualSystem vs = createVirtualSystem(baseName, MGR_TYPE);

        SecurityGroup sg = new SecurityGroup(vs.getVirtualizationConnector(), null, null);
        sg.setName(baseName + "_sg");

        SecurityGroupInterface sgi = new SecurityGroupInterface(vs, null, "1", FailurePolicyType.FAIL_CLOSE, 1L);
        sgi.setName(baseName + "_sgi");
        sgi.setSecurityGroup(sg);
        sg.addSecurityGroupInterface(sgi);
        sgi.setMarkedForDeletion(isDelete);

        return sgi;
    }

    public static void persist(SecurityGroupInterface sgi, EntityManager em) {
        em.getTransaction().begin();

        SecurityGroup sg = sgi.getSecurityGroup();
        VirtualizationConnector vc = sg.getVirtualizationConnector();
        Set<VirtualSystem> virtualSystems = vc.getVirtualSystems();
        em.persist(vc);

        for (VirtualSystem vs : virtualSystems) {
            em.persist(vs.getDomain().getApplianceManagerConnector());
            em.persist(vs.getApplianceSoftwareVersion().getAppliance());
            em.persist(vs.getApplianceSoftwareVersion());
            em.persist(vs.getDistributedAppliance());
            em.persist(vs.getDomain());
            em.persist(vs);
        }

        em.persist(sg);

        em.persist(sgi);

        em.getTransaction().commit();
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
