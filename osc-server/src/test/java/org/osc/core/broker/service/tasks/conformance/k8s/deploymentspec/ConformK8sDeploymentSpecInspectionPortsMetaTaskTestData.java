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
import java.util.List;
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
import org.osc.core.common.virtualization.VirtualizationType;

public class ConformK8sDeploymentSpecInspectionPortsMetaTaskTestData {
    public static List<DeploymentSpec> TEST_DEPLOYMENT_SPECS = new ArrayList<>();

    public static DeploymentSpec DS_NO_DAIS =
            createDS("DS_NO_DAIS");

    public static DeploymentSpec DS_WITH_ONLY_ORPHAN_DAIS =
            createDSWithDAIs("DS_WITH_ONLY_ORPHAN_DAIS", 0, 2);


    public static DeploymentSpec DS_WITHOUT_ORPHAN_DAIS =
            createDSWithDAIs("DS_WITHOUT_ORPHAN_DAIS", 2, 0);


    public static DeploymentSpec DS_WITH_SOME_ORPHAN_DAIS_SOME_ASSIGNED_DAIS =
            createDSWithDAIs("DS_WITH_SOME_ORPHAN_DAIS_SOME_ASSIGNED_DAIS", 2, 2);


    public static TaskGraph deleteDAIsInspectionPortGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();

        for(DistributedApplianceInstance dai : ds.getDistributedApplianceInstances()) {
            expectedGraph.addTask(new DeleteK8sDAIInspectionPortTask().create(dai));
        }

        return expectedGraph;
    }

    public static TaskGraph registerDAIsInspectionPortGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();

        for(DistributedApplianceInstance dai : ds.getDistributedApplianceInstances()) {
            expectedGraph.addTask(new RegisterK8sDAIInspectionPortTask().create(dai));
        }

        return expectedGraph;
    }

    public static TaskGraph emptyGraph() {
        return new TaskGraph();
    }

    public static TaskGraph deleteAndRegiserDAIsInspectionPortGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();

        for(DistributedApplianceInstance dai : ds.getDistributedApplianceInstances()) {
            if (dai.getInspectionOsIngressPortId() == null) {
                expectedGraph.addTask(new DeleteK8sDAIInspectionPortTask().create(dai));
            } else {
                expectedGraph.addTask(new RegisterK8sDAIInspectionPortTask().create(dai));
            }
        }

        return expectedGraph;
    }

    private static void addDAIToDS(DeploymentSpec ds, String baseName, String inspectionIngressPortId) {
        DistributedApplianceInstance dai = new DistributedApplianceInstance(ds.getVirtualSystem());
        dai.setDeploymentSpec(ds);
        dai.setInspectionOsIngressPortId(inspectionIngressPortId);
        dai.setName(baseName + "_DAI_" + UUID.randomUUID().toString());

        ds.getDistributedApplianceInstances().add(dai);
    }

    private static DeploymentSpec createDSWithDAIs(String baseName, int countDAISWithNetInfo, int countDAISWithoutNetInfo) {
        DeploymentSpec ds = createDS(baseName);

        for (;countDAISWithNetInfo > 0; countDAISWithNetInfo--) {
            addDAIToDS(ds, baseName, UUID.randomUUID().toString());
        }

        for (;countDAISWithoutNetInfo > 0; countDAISWithoutNetInfo--) {
            addDAIToDS(ds, baseName, null);
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
