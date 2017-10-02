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
package org.osc.core.broker.service.tasks.conformance.openstack.sfc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.common.virtualization.VirtualizationType;

public class CreateServiceFunctionChainTaskTestData {
	public static boolean DB_POPULATED = false;

	private static final ServiceFunctionChain SERVICE_FUCNTION_CHAIN = createSFC();
	private static final VirtualizationConnector VIRTUALIZATION_CONNECTOR = createVC();

	static final SecurityGroup SECURITY_GROUP_SFC_BINDED = createSG("SG_1", SERVICE_FUCNTION_CHAIN,
			VIRTUALIZATION_CONNECTOR, "PROJECT_ID_1", "NETWORK_ELEMENT_ID_1");
	static final SecurityGroup NEW_SECURITY_GROUP_SAME_SFC_BINDED_UPDATE_ELEMENT_ID = createSG("SG_2",
			SERVICE_FUCNTION_CHAIN, VIRTUALIZATION_CONNECTOR, "PROJECT_ID_1", null);
	static final SecurityGroup SECURITY_GROUP_SFC_BINDED_EXPECT_CREATE = createSG("SG_3",
			SERVICE_FUCNTION_CHAIN, VIRTUALIZATION_CONNECTOR, "PROJECT_ID_2", "NETWORK_ELEMENT_ID_2");
	static final SecurityGroup SECURITY_GROUP_SFC = createSG("SG_4", SERVICE_FUCNTION_CHAIN,
			VIRTUALIZATION_CONNECTOR, "PROJECT_ID_3", null);

	static void persist(SecurityGroup sg, EntityManager em) {
		VirtualizationConnector vc = sg.getVirtualizationConnector();
		Set<VirtualSystem> virtualSystems = vc.getVirtualSystems();
		ServiceFunctionChain sfc = sg.getServiceFunctionChain();
		// If the entity already has an id it already has been persisted.
		if (sg.getId() == null) {
			if (vc.getId() == null) {
				em.persist(vc);
			}
			em.persist(sg);
			if (sfc.getId() == null) {
				em.persist(sfc);
			}

			for (VirtualSystem vs : virtualSystems) {
				em.persist(vs.getDomain().getApplianceManagerConnector());
				em.persist(vs.getApplianceSoftwareVersion().getAppliance());
				em.persist(vs.getApplianceSoftwareVersion());
				em.persist(vs.getDistributedAppliance());
				em.persist(vs.getDomain());
				em.persist(vs);
				for (DeploymentSpec ds : vs.getDeploymentSpecs()) {
					em.persist(ds);
				}
			}
		}
	}

	private static ServiceFunctionChain createSFC() {
		ServiceFunctionChain sfc = new ServiceFunctionChain();
		sfc.setName("sfc");
		return sfc;
	}

	private static VirtualizationConnector createVC() {
		VirtualizationConnector vc = new VirtualizationConnector();
		vc.setName("vc");
		vc.setVirtualizationType(VirtualizationType.OPENSTACK);
		vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
		vc.setProviderIpAddress("providerIp");
		vc.setProviderUsername("foo");
		vc.setProviderPassword("********");
		return vc;
	}

	private static SecurityGroup createSG(String baseName, ServiceFunctionChain sfc, VirtualizationConnector vc,
			String projectId, String networkElementId) {
		VirtualSystem vs = createVirtualSystem(baseName, "NSM", vc);

		SecurityGroup sg = new SecurityGroup(vs.getVirtualizationConnector(), projectId, "PROJECT_NAME");
		sg.setName(baseName + "_sg");
		sg.setNetworkElementId(networkElementId);

		sfc.addVirtualSystem(vs);
		sfc.setVirtualizationConnector(vs.getVirtualizationConnector());
		sg.setServiceFunctionChain(sfc);

		return sg;
	}

	private static VirtualSystem createVirtualSystem(String baseName, String mgrType, VirtualizationConnector vc) {
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

		DeploymentSpec ds = new DeploymentSpec(vs, null, "PROJECT_ID", null, null, null);
		ds.setName(baseName + "_ds");
		ds.setPortGroupId("PORT_PAIR_GROUP");
		ds.setInstanceCount(1);

		vs.setDeploymentSpecs(new HashSet<>(Arrays.asList(ds)));

		vc.getVirtualSystems().add(vs);
		return vs;
	}
}
