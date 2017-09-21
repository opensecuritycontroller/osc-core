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
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.common.virtualization.VirtualizationType;

public class CreateOrUpdateK8sDeploymentSpecMetaTaskTestData {
    public static List<DeploymentSpec> TEST_DEPLOYMENT_SPECS = new ArrayList<>();

    public static DeploymentSpec UPDATE_DS = createDS("UPDATE_DS");

    public static DeploymentSpec CREATE_DS_NO_EXTERNAL_ID =
            createDSNoExternalId("CREATE_DS_NO_EXTERNAL_ID");

    public static DeploymentSpec CREATE_DS_NO_K8S_DEPLOYMENT =
            createDS("CREATE_DS_NO_K8S_DEPLOYMENT");

    public static DeploymentSpec UPDATE_DS_NEW_INSTANCE_COUNT =
            createDS("UPDATE_DS_NEW_INSTANCE_COUNT");

    public static DeploymentSpec NOOP_DS_SAME_INSTANCE_COUNT =
            createDS("NOOP_DS_SAME_INSTANCE_COUNT");

    private static DeploymentSpec createDSNoExternalId(String dsName) {
        DeploymentSpec ds = createDS(dsName);
        ds.setExternalId(null);
        return ds;
    }

    public static TaskGraph createDSGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new CreateK8sDeploymentTask().create(ds));

        expectedGraph.appendTask(new CheckK8sDeploymentStateTask().create(ds));
        expectedGraph.appendTask(new ConformK8sDeploymentPodsMetaTask().create(ds));

        return expectedGraph;
    }

    public static TaskGraph updateDSGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new UpdateK8sDeploymentTask().create(ds));

        expectedGraph.appendTask(new CheckK8sDeploymentStateTask().create(ds));
        expectedGraph.appendTask(new ConformK8sDeploymentPodsMetaTask().create(ds));

        return expectedGraph;
    }

    public static TaskGraph emptyDSGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new CheckK8sDeploymentStateTask().create(ds));
        expectedGraph.appendTask(new ConformK8sDeploymentPodsMetaTask().create(ds));
        return expectedGraph;
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
