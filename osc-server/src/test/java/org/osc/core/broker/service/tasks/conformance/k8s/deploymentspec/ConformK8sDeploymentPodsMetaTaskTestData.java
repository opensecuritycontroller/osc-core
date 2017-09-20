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
package org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.common.job.TaskGuard;
import org.osc.core.common.virtualization.VirtualizationType;

public class ConformK8sDeploymentPodsMetaTaskTestData {
    public static List<DeploymentSpec> TEST_DEPLOYMENT_SPECS = new ArrayList<>();

    private static List<String> KNOWN_POD_IDS = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());

    public static List<KubernetesPod> ORPHAN_PODS = Arrays.asList(createKubernetesPod(), createKubernetesPod(), createKubernetesPod());

    public static List<KubernetesPod> MATCHING_PODS = Arrays.asList(createKubernetesPod(KNOWN_POD_IDS.get(0)), createKubernetesPod(KNOWN_POD_IDS.get(1)));

    public static DeploymentSpec DS_NO_DAI_ORPHAN_PODS =
            createDS("DS_NO_DAI_ORPHAN_PODS");

    public static DeploymentSpec DS_ORPHAN_DAIS_NO_PODS =
            createDSWithDAIs("DS_ORPHAN_DAIS_NO_PODS", 3, false);

    public static DeploymentSpec DS_SOME_ORPHAN_DAIS_SOME_ORPHAN_PODS =
            createDSWithDAIs("DS_SOME_ORPHAN_DAIS_SOME_ORPHAN_PODS", 2, true);

    public static DeploymentSpec DS_DAIS_PODS_MATCHING =
            createDSWithDAIs("DS_DAIS_PODS_MATCHING", 0, true);

    public static TaskGraph conformOrphanK8sPodsAsDaisGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();

        for(KubernetesPod pod : ORPHAN_PODS) {
            expectedGraph.addTask(new CreateOrUpdateK8sDAITask().create(ds, pod));
        }

        expectedGraph.appendTask(new ConformK8sInspectionPortMetaTask().create(ds), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask().create(ds.getVirtualSystem()), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    public static TaskGraph conformOrphanDaisGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();

        for(DistributedApplianceInstance dai : ds.getDistributedApplianceInstances()) {
            expectedGraph.addTask(new DeleteOrCleanK8sDAITask().create(ds, dai));
        }

        expectedGraph.appendTask(new ConformK8sInspectionPortMetaTask().create(ds), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask().create(ds.getVirtualSystem()), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    public static TaskGraph conformOrphanDaisAndOrphanPodsGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();

        for(KubernetesPod pod : ORPHAN_PODS) {
            expectedGraph.addTask(new CreateOrUpdateK8sDAITask().create(ds, pod));
        }

        for(DistributedApplianceInstance dai : ds.getDistributedApplianceInstances()) {
            if (!KNOWN_POD_IDS.contains(dai.getExternalId())) {
                expectedGraph.addTask(new DeleteOrCleanK8sDAITask().create(ds, dai));
            }
        }

        expectedGraph.appendTask(new ConformK8sInspectionPortMetaTask().create(ds), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask().create(ds.getVirtualSystem()), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    public static TaskGraph conformDaisMatchingPodsGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();

        expectedGraph.appendTask(new ConformK8sInspectionPortMetaTask().create(ds), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new MgrCheckDevicesMetaTask().create(ds.getVirtualSystem()), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    public static TaskGraph emptyDSGraph() {
        TaskGraph expectedGraph = new TaskGraph();
        return expectedGraph;
    }

    private static KubernetesPod createKubernetesPod() {
        return createKubernetesPod(null);
    }

    private static KubernetesPod createKubernetesPod(String podId) {
        KubernetesPod k8sPod = new KubernetesPod("name", "namespace", podId == null ? UUID.randomUUID().toString() : podId, "node");
        return k8sPod;
    }

    private static void addDAIToDS(DeploymentSpec ds, String baseName, String externalId) {
        DistributedApplianceInstance dai = new DistributedApplianceInstance(ds.getVirtualSystem());
        dai.setDeploymentSpec(ds);
        dai.setExternalId(externalId == null ? UUID.randomUUID().toString() : externalId);
        dai.setName(baseName + "_DAI" + dai.getExternalId());

        Set<DistributedApplianceInstance> dais = ds.getDistributedApplianceInstances();
        dais.add(dai);

        ds.setDistributedApplianceInstances(dais);
    }

    private static DeploymentSpec createDSWithDAIs(String baseName, int countOrphanDais, boolean includeMatchingDais) {
        DeploymentSpec ds = createDS(baseName);

        for (;countOrphanDais > 0; countOrphanDais--) {
            addDAIToDS(ds, baseName, null);
        }

        if (includeMatchingDais) {
            for (String KNOWN_POD_ID : KNOWN_POD_IDS) {
                addDAIToDS(ds, baseName, KNOWN_POD_ID);
            }
        }

        return ds;
    }

    private static DeploymentSpec createDS(String baseName) {
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

        DeploymentSpec ds = new DeploymentSpec(vs, null, null, null, null, null);
        ds.setName(baseName + "_ds");
        ds.setNamespace(UUID.randomUUID().toString());
        ds.setExternalId(UUID.randomUUID().toString());

        TEST_DEPLOYMENT_SPECS.add(ds);
        return ds;
    }
}
