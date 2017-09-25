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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
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

public class UpdateK8sSecurityGroupMemberLabelMetaTaskTestData {

    private static final String MGR_TYPE = "NSM";

    private static List<String> KNOWN_POD_IDS = Arrays.asList(UUID.randomUUID().toString(),
            UUID.randomUUID().toString());

    public static List<KubernetesPod> MATCHING_PODS = Arrays.asList(createKubernetesPod(KNOWN_POD_IDS.get(0)),
            createKubernetesPod(KNOWN_POD_IDS.get(1)));

    public static SecurityGroupMember NO_ENTITY_ORPHAN_PODS_SGM = createSGM("NO_ENTITY_ORPHAN_PODS");

    public static SecurityGroupMember ORPHAN_ENTITIES_NO_PODS_SGM = createSGMWithMembers("ORPHAN_ENTITIES_NO_PODS", 3,
            false);

    public static SecurityGroupMember SOME_ORPHAN_ENTITIES_SOME_ORPHAN_PODS_SGM = createSGMWithMembers(
            "SOME_ORPHAN_DAIS_SOME_ORPHAN_PODS", 2, true);

    public static SecurityGroupMember ENTITIES_PODS_MATCHING_SGM = createSGMWithMembers("DAIS_PODS_MATCHING", 0, true);

    public static TaskGraph createExpectedGraph(SecurityGroupMember sgm) {
        TaskGraph expectedGraph = new TaskGraph();

        Label label = sgm.getLabel();

        Collection<String> podIdsInDB = label.getPods().stream().map(Pod::getExternalId).collect(Collectors.toList());
        for (KubernetesPod kp : MATCHING_PODS) {
            if (!podIdsInDB.contains(kp.getUid())) {
                expectedGraph.addTask(new CreateK8sLabelPodTask().create(kp, label));
            }
        }

        for (Pod p : label.getPods()) {
            if (!KNOWN_POD_IDS.contains(p.getExternalId())) {
                expectedGraph.addTask(new LabelPodDeleteTask().create(p));
            }
        }

        return expectedGraph;
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

    private static KubernetesPod createKubernetesPod(String podId) {
        KubernetesPod k8sPod = new KubernetesPod("name", "namespace",
                podId == null ? UUID.randomUUID().toString() : podId, "node");
        return k8sPod;
    }

    private static void addPodToLabel(Label label, String baseName, String externalId) {
        Pod pod = new Pod(baseName + "_pod", baseName + "_namespace", UUID.randomUUID().toString(), externalId == null ? UUID.randomUUID().toString() : externalId);
        pod.getLabels().add(label);
    }

    private static SecurityGroupMember createSGM(String baseName) {
        VirtualSystem vs = createVirtualSystem(baseName, MGR_TYPE);

        SecurityGroup sg = new SecurityGroup(vs.getVirtualizationConnector(), null, null);
        sg.setName(baseName + "_sg ");

        SecurityGroupMember sgm = new SecurityGroupMember(sg, new Label("LABEL_name" + UUID.randomUUID(),
                OSC_DEPLOYMENT_LABEL_NAME + "=LABEL_value" + UUID.randomUUID()));

        return sgm;
    }

    private static SecurityGroupMember createSGMWithMembers(String baseName, int countOrphanPods,
            boolean includeMatchingPods) {

        SecurityGroupMember sgm = createSGM(baseName);
        for (; countOrphanPods > 0; countOrphanPods--) {
            addPodToLabel(sgm.getLabel(), baseName, null);
        }

        if (includeMatchingPods) {
            for (String KNOWN_POD_ID : KNOWN_POD_IDS) {
                addPodToLabel(sgm.getLabel(), baseName, KNOWN_POD_ID);
            }
        }

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
