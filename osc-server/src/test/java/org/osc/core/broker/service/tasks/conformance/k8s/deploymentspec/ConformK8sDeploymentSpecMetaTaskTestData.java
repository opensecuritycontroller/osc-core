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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DeleteDSFromDbTask;
import org.osc.core.common.virtualization.VirtualizationType;

public class ConformK8sDeploymentSpecMetaTaskTestData {
    public static List<DeploymentSpec> TEST_DEPLOYMENT_SPECS = new ArrayList<>();

    public static DeploymentSpec UPDATE_DS = createDS("UPDATE_DS");

    public static DeploymentSpec DELETE_DS_MARKED_FOR_DELETION =
            createDSMarkedForDeletion("DELETE_DS_MARKED_FOR_DELETION");

    public static DeploymentSpec DELETE_DS_MARKED_DELETION_WITH_DAIS =
            createDSMarkedForDeletionWithDais("DELETE_DS_MARKED_DELETION_WITH_DAIS", "DELETE_DS_MARKED_DELETION_WITH_DAIS_DAI", 2);

    public static DeploymentSpec DELETE_DS_VS_MARKED_FOR_DELETION =
            createDSVSMarkedForDeletion("DELETE_DS_VS_MARKED_FOR_DELETION");

    public static DeploymentSpec DELETE_DS_DA_MARKED_FOR_DELETION =
            createDSDAMarkedForDeletion("DELETE_DS_DA_MARKED_FOR_DELETION");

    private static DeploymentSpec createDSMarkedForDeletion(String dsName) {
        DeploymentSpec ds = createDS(dsName);
        ds.setMarkedForDeletion(true);
        return ds;
    }

    private static DeploymentSpec createDSVSMarkedForDeletion(String dsName) {
        DeploymentSpec ds = createDS(dsName);
        ds.getVirtualSystem().setMarkedForDeletion(true);
        return ds;
    }

    private static DeploymentSpec createDSDAMarkedForDeletion(String dsName) {
        DeploymentSpec ds = createDS(dsName);
        ds.getVirtualSystem().getDistributedAppliance().setMarkedForDeletion(true);
        return ds;
    }

    private static DeploymentSpec createDSWithDais(String dsName, String daiName, int daiCount) {
        DeploymentSpec ds = createDS(dsName);
        Set<DistributedApplianceInstance> dais = new HashSet<>();
        for (;daiCount > 0; daiCount--) {
            DistributedApplianceInstance dai = new DistributedApplianceInstance(ds.getVirtualSystem());
            dai.setDeploymentSpec(ds);
            dai.setName(daiName + daiCount);
            dais.add(dai);
            ds.getVirtualSystem().addDistributedApplianceInstance(dai);
        }

        ds.setDistributedApplianceInstances(dais);

        return ds;
    }

    private static DeploymentSpec createDSMarkedForDeletionWithDais(String dsName, String daiName, int daiCount) {
        DeploymentSpec ds = createDSWithDais(dsName, daiName, daiCount);
        ds.setMarkedForDeletion(true);
        return ds;
    }

    public static TaskGraph deleteDSGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new DeleteK8sDeploymentTask().create(ds));

        for (DistributedApplianceInstance dai : ds.getDistributedApplianceInstances()) {
            expectedGraph.addTask(new DeleteK8sDAIInspectionPortTask().create(dai));
        }

        expectedGraph.appendTask(new MgrCheckDevicesMetaTask().create(ds.getVirtualSystem()));
        expectedGraph.appendTask(new DeleteDSFromDbTask().create(ds));

        return expectedGraph;
    }

    public static TaskGraph updateDSGraph(DeploymentSpec ds) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new CreateOrUpdateK8sDeploymentSpecMetaTask().create(ds));
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

        TEST_DEPLOYMENT_SPECS.add(ds);
        return ds;
    }
}
