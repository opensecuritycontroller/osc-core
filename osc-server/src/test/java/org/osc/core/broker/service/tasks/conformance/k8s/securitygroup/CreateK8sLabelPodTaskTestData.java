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
import java.util.UUID;

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
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.common.virtualization.VirtualizationType;

public class CreateK8sLabelPodTaskTestData {
    public static boolean DB_POPULATED = false;
    private static final String ALREADY_PROTECTED_POD_ID = UUID.randomUUID().toString();

    public static final KubernetesPod ALREADY_PROTECTED_K8S_POD = createKubernetesPod(ALREADY_PROTECTED_POD_ID);
    public static final KubernetesPod NETWORK_ELEMENT_NOT_FOUND_K8S_POD = createKubernetesPod();
    public static final KubernetesPod VALID_K8S_POD = createKubernetesPod();

    public static final Label EXISTING_PROTECTED_POD_SGM_LABEL = createSGMWithPod("EXISTING_PROTECTED_POD_SGM_LABEL");
    public static final Label VALID_POD_SGM_LABEL = createSGM("VALID_POD_SGM");
    public static final Label ALREADY_PROTECTED_POD_SGM_LABEL = createSGM("ALREADY_PROTECTED_POD_SGM_LABEL");
    public static final Label NETWORK_ELEMENT_NOT_FOUND_POD_SGM_LABEL = createSGM("NETWORK_ELEMENT_NOT_FOUND_POD_SGM_LABEL");

    public static void persist(Label label, EntityManager em) {
        for (Pod pod : label.getPods()) {
            em.persist(pod);
        }
        em.persist(label);
        persist(label.getSecurityGroupMembers().iterator().next(), em);

    }

    public static void cleanUp(Label label) {
        for (Pod pod : label.getPods()) {
            pod.setId(null);
        }
        label.setId(null);
    }

    private static void persist(SecurityGroupMember sgm, EntityManager em) {
        SecurityGroup sg = sgm.getSecurityGroup();
        VirtualizationConnector vc = sg.getVirtualizationConnector();
        Set<VirtualSystem> virtualSystems = vc.getVirtualSystems();
        em.persist(vc);
        em.persist(sg);

        for (VirtualSystem vs : virtualSystems) {
            em.persist(vs.getDomain().getApplianceManagerConnector());
            em.persist(vs.getApplianceSoftwareVersion().getAppliance());
            em.persist(vs.getApplianceSoftwareVersion());
            em.persist(vs.getDistributedAppliance());
            em.persist(vs.getDomain());
            em.persist(vs);
        }

        for (Pod pod : sgm.getLabel().getPods()) {
            em.persist(pod);
        }

        em.persist(sgm.getLabel());
        em.persist(sgm);
    }

    private static KubernetesPod createKubernetesPod() {
        return createKubernetesPod(null);
    }

    private static KubernetesPod createKubernetesPod(String podId) {
        KubernetesPod k8sPod = new KubernetesPod(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                podId == null ? UUID.randomUUID().toString() : podId, UUID.randomUUID().toString());
        return k8sPod;
    }

    private static Label createSGM(String baseName) {
        VirtualSystem vs = createVirtualSystem(baseName, "NSM");

        SecurityGroup sg = new SecurityGroup(vs.getVirtualizationConnector(), null, null);
        sg.setName(baseName + "_sg ");

        Label label = new Label(UUID.randomUUID().toString(),UUID.randomUUID().toString());

        SecurityGroupMember sgm = new SecurityGroupMember(sg, label);
        label.getSecurityGroupMembers().add(sgm);

        return label;
    }

    private static Label createSGMWithPod(String baseName) {
        Label newLabel = createSGM(baseName);
        Pod existingPod = new Pod(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), ALREADY_PROTECTED_POD_ID);
        newLabel.getPods().add(existingPod);
        existingPod.getLabels().add(newLabel);
        return newLabel;
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
