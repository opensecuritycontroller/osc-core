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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.sdn.NetworkElementImpl;
import org.osc.core.broker.model.sdn.PortPairGroupNetworkElementImpl;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CheckServiceFunctionChainMetaTask.class)
public class CheckServiceFunctionChainMetaTask extends TransactionalMetaTask {
	@Reference
	CreateServiceFunctionChainTask createServiceFunctionChainTask;

	@Reference
	UpdateServiceFunctionChainTask updateServiceFunctionChainTask;

	@Reference
	DeleteServiceFunctionChainTask deleteServiceFunctionChainTask;

	@Reference
	private ApiFactoryService apiFactoryService;

	private SecurityGroup securityGroup;
	TaskGraph tg;
	private List<NetworkElement> portPairGroups;

	public CheckServiceFunctionChainMetaTask create(SecurityGroup sg) {
		CheckServiceFunctionChainMetaTask task = new CheckServiceFunctionChainMetaTask();

		task.createServiceFunctionChainTask = this.createServiceFunctionChainTask;
		task.updateServiceFunctionChainTask = this.updateServiceFunctionChainTask;
		task.deleteServiceFunctionChainTask = this.deleteServiceFunctionChainTask;
		task.securityGroup = sg;
		task.apiFactoryService = this.apiFactoryService;
		task.dbConnectionManager = this.dbConnectionManager;
		task.txBroadcastUtil = this.txBroadcastUtil;

		return task;
	}

	@Override
	public void executeTransaction(EntityManager em) throws Exception {
		this.tg = new TaskGraph();
		this.securityGroup = em.find(SecurityGroup.class, this.securityGroup.getId());

		if (this.securityGroup.getNetworkElementId() == null && this.securityGroup.getServiceFunctionChain() != null) {
			// if SFC binded and network element null -> create PC
			this.portPairGroups = getPortPairGroupWithSameProjectId(em, this.securityGroup.getProjectId(),
					this.securityGroup.getServiceFunctionChain().getVirtualSystems());

			this.tg.appendTask(this.createServiceFunctionChainTask.create(this.securityGroup, this.portPairGroups));

		} else if (this.securityGroup.getNetworkElementId() != null && this.securityGroup.getServiceFunctionChain() != null) {
			// if SFC binded and network element not null -> update PC
			this.portPairGroups = getPortPairGroupWithSameProjectId(em, this.securityGroup.getProjectId(),
					this.securityGroup.getServiceFunctionChain().getVirtualSystems());

			try (SdnRedirectionApi sdnApi = this.apiFactoryService
					.createNetworkRedirectionApi(this.securityGroup.getVirtualizationConnector())) {

				List<NetworkElement> sfcPortPairGroups = sdnApi
						.getNetworkElements(new NetworkElementImpl(this.securityGroup.getNetworkElementId()));

				if (!this.portPairGroups.equals(sfcPortPairGroups)) {
					this.tg.appendTask(
							this.updateServiceFunctionChainTask.create(this.securityGroup, this.portPairGroups));
				}
			}

		} else if (this.securityGroup.getNetworkElementId() != null && this.securityGroup.getServiceFunctionChain() == null) {
			// if SFC not binded and network element not null -> delete PC
			this.tg.appendTask(this.deleteServiceFunctionChainTask.create(this.securityGroup));

		}
	}

	static List<NetworkElement> getPortPairGroupWithSameProjectId(EntityManager em, String projectId,
			List<VirtualSystem> vsList) throws VmidcBrokerValidationException {

		List<NetworkElement> portPairGroups = new ArrayList<>();

		for (VirtualSystem vs : vsList) {

			// Currently, We have no information on OSC SG about the OST region. SFC do not support port chain with multiple region deployment.
			// We cannot create Port Chain of VNF Deployment's belonging to different region. If we find the VS with multiple DS on different region throw an exception.
			List<DeploymentSpec> dsList = DeploymentSpecEntityMgr
					.findDeploymentSpecsByVirtualSystemProjectWithDefaultRegionOne(em, vs, projectId);

			if (dsList.isEmpty()) {
				throw new VmidcBrokerValidationException(
						String.format("No deployment found for virtual system '%s'.", vs.getName()));
			}

			if (dsList.size() > 1) {
				throw new VmidcBrokerValidationException(String
						.format("Multiple deployments found for vs '%s' belonging to the same project.", vs.getName()));
			}

			if (dsList.iterator().next().getPortGroupId() == null) {
				throw new VmidcBrokerValidationException(String.format(
						"No port pair group found. Deployment spec '%s' is expected to have port pair group for vs '%s'.",
						dsList.iterator().next().getName(), vs.getName()));
			}
			portPairGroups.add(new PortPairGroupNetworkElementImpl(dsList.iterator().next().getPortGroupId()));

		}
		return portPairGroups;
	}

	@Override
	public Set<LockObjectReference> getObjects() {
		return LockObjectReference.getObjectReferences(this.securityGroup);
	}

	@Override
	public TaskGraph getTaskGraph() {
		return this.tg;
	}

	@Override
	public String getName() {
		return String.format("Checking Service Function Chain for Security Group '%s'", this.securityGroup.getName());
	}

}
