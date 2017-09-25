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

import static org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi.OSC_DEPLOYMENT_LABEL_NAME;

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

public class LabelPodCreateTaskTestData {

    public static final KubernetesPod NEW_UNKNOWN_POD_NO_OTHER_LABEL = createKubernetesPod();
    public static final KubernetesPod NEW_UNKNOWN_POD_WITH_OTHER_UNPROTECTED_LABEL = createKubernetesPod();
    public static final KubernetesPod NEW_KNOWN_POD_WITH_OTHER_UNPROTECTED_LABEL = createKubernetesPod();
    public static final KubernetesPod NEW_KNOWN_POD_WITH_OTHER_PROTECTED_LABEL = createKubernetesPod();

    // Not possible: if the other label is protected under a security group, we know about the pod
    //  public static final KubernetesPod NEW_UNKNOWN_POD_WITH_OTHER_PROTECTED_LABEL = createKubernetesPod();

    public static final Label OTHER_LABEL_WITH_KNOWN_POD_ENTITY
        = createOtherLabelWithPodEntity(NEW_KNOWN_POD_WITH_OTHER_UNPROTECTED_LABEL.getUid());
    public static final SecurityGroupMember OTHEL_LABEL_SGM = createSGMProtectingPod(NEW_KNOWN_POD_WITH_OTHER_PROTECTED_LABEL);

    public static final String OTHER_UNKNOWN_LABEL_VALUE = OSC_DEPLOYMENT_LABEL_NAME  + "=OTHER_LABEL_value";

    public static Label createLabelUnderTest() {
        return new Label(UUID.randomUUID().toString(), OSC_DEPLOYMENT_LABEL_NAME  + (UUID.randomUUID().toString()));
    }
    private static SecurityGroupMember createSGMProtectingPod(KubernetesPod kubernetesPod) {
        String baseName = "OTHER_";
        SecurityGroupMember sgm = createSGM(baseName);
        addPodToLabel(sgm.getLabel(), baseName, kubernetesPod.getUid());
        return sgm;
    }

    private static Label createOtherLabelWithPodEntity(String externalId) {
        String baseName = "OTHER_";
        Label label = new Label(baseName + "_LABEL_name", OSC_DEPLOYMENT_LABEL_NAME  + "=" + baseName + "_LABEL_value");
        addPodToLabel(label, baseName, externalId);
        return label;
    }

    public static void persist(Label label, EntityManager em) {
        em.getTransaction().begin();
        for (Pod pod : label.getPods()) {
            em.persist(pod);
        }
        em.persist(label);
        em.getTransaction().commit();
    }

    public static void persist(SecurityGroupMember sgm, EntityManager em) {
        em.getTransaction().begin();

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

        em.getTransaction().commit();
    }

    private static KubernetesPod createKubernetesPod() {
        return createKubernetesPod(null);
    }

    private static KubernetesPod createKubernetesPod(String podId) {
        KubernetesPod k8sPod = new KubernetesPod("name", "namespace",
                podId == null ? UUID.randomUUID().toString() : podId, "node");
        return k8sPod;
    }

    private static void addPodToLabel(Label label, String baseName, String externalId) {
        Pod pod = new Pod();
        pod.setName(baseName + "_pod");
        pod.setNamespace(baseName + "_namespace");
        pod.setExternalId(externalId == null ? UUID.randomUUID().toString() : externalId);
        pod.getLabels().add(label);
    }

    private static SecurityGroupMember createSGM(String baseName) {
        VirtualSystem vs = createVirtualSystem(baseName, "NSM");

        SecurityGroup sg = new SecurityGroup(vs.getVirtualizationConnector(), null, null);
        sg.setName(baseName + "_sg ");

        SecurityGroupMember sgm = new SecurityGroupMember(sg, new Label("LABEL_name" + UUID.randomUUID(),
                OSC_DEPLOYMENT_LABEL_NAME + "=LABEL_value" + UUID.randomUUID()));

        return sgm;
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
