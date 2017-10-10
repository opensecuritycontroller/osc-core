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

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CreateServiceFunctionChainTask.class)
public class CreateServiceFunctionChainTask extends TransactionalTask {

	@Reference
	private ApiFactoryService apiFactory;

	private ServiceFunctionChain sfc;
	private SecurityGroup securityGroup;
	private List<NetworkElement> portPairGroups;

	public CreateServiceFunctionChainTask create(SecurityGroup securityGroup, List<NetworkElement> portPairGroups) {
		CreateServiceFunctionChainTask task = new CreateServiceFunctionChainTask();

		task.sfc = securityGroup.getServiceFunctionChain();
		task.securityGroup = securityGroup;
		task.portPairGroups = portPairGroups;
		task.apiFactory = this.apiFactory;
		task.name = task.getName();
		task.dbConnectionManager = this.dbConnectionManager;
		task.txBroadcastUtil = this.txBroadcastUtil;

		return task;
	}

	@Override
	public void executeTransaction(EntityManager em) throws Exception {
		this.sfc = em.find(ServiceFunctionChain.class, this.sfc.getId());
		this.securityGroup = em.find(SecurityGroup.class, this.securityGroup.getId());

		List<SecurityGroup> existingSGList = SecurityGroupEntityMgr.listOtherSecurityGroupsWithSameSFC(em,
				this.securityGroup);

		if (existingSGList.isEmpty()) {

			try (SdnRedirectionApi sdnApi = this.apiFactory
					.createNetworkRedirectionApi(this.securityGroup.getVirtualizationConnector())) {

				NetworkElement sfcChain = sdnApi.registerNetworkElement(this.portPairGroups);
				this.securityGroup.setNetworkElementId(sfcChain.getElementId());
			}
		} else {
			this.securityGroup.setNetworkElementId(existingSGList.iterator().next().getNetworkElementId());
		}
		em.merge(this.securityGroup);
	}

	@Override
	public Set<LockObjectReference> getObjects() {
		return LockObjectReference.getObjectReferences(this.securityGroup, this.sfc);
	}

	@Override
	public String getName() {
		return String.format("Creating Service Function Chain '%s' for Security Group '%s' under Project '%s'",
				this.sfc.getName(), this.securityGroup.getName(), this.securityGroup.getProjectName());
	}

}
