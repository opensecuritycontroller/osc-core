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
package org.osc.core.broker.service.tasks.conformance.k8s.securitygroup;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.common.virtualization.VirtualizationType;

public class CheckK8sSecurityGroupLabelMetaTaskTestData {

    private static final String CREATED = "CREATED";
    private static final String DELETED = "DELETED";

    public static final SecurityGroupMember CREATED_SGM = createSecurityGroupMember(CREATED, false);
    public static final SecurityGroupMember DELETED_SGM = createSecurityGroupMember(DELETED, true);

    public static void persist(SecurityGroupMember sgm, EntityManager em) {
        SecurityGroup sg = sgm.getSecurityGroup();
        em.getTransaction().begin();

        Set<VirtualSystem> virtualSystems = sg.getVirtualizationConnector().getVirtualSystems();
        em.persist(sg.getVirtualizationConnector());

        for (VirtualSystem vs : virtualSystems) {
            em.persist(vs.getDomain().getApplianceManagerConnector());
            em.persist(vs.getApplianceSoftwareVersion().getAppliance());
            em.persist(vs.getApplianceSoftwareVersion());
            em.persist(vs.getDistributedAppliance());
            em.persist(vs.getDomain());
            em.persist(vs);
        }

        em.persist(sgm.getLabel());
        em.persist(sg);
        em.persist(sgm);

        em.getTransaction().commit();
    }

    private static SecurityGroupMember createSecurityGroupMember(String baseName, boolean isDelete) {
        VirtualSystem vs = createVirtualSystem(baseName, "SMC");

        SecurityGroup sg = new SecurityGroup(vs.getVirtualizationConnector(), null, null);
        sg.setName(baseName + "_sg");
        sg.setMarkedForDeletion(isDelete);

        Label label = new Label("LABEL_NAME_" +  sg.getName(), "LABEL_VALUE_" + sg.getName());
        return new SecurityGroupMember(sg, label);
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
