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
package org.osc.core.broker.model.plugin.manager;

import java.util.Set;

import org.osc.sdk.manager.element.ManagerPolicyElement;
import org.osc.sdk.manager.element.SecurityGroupInterfaceElement;

public class SecurityGroupInterfaceElementImpl implements SecurityGroupInterfaceElement {

	private String managerSecurityGroupInterfaceId;
	private String name;
	private String managerSecurityGroupId;
	private Set<ManagerPolicyElement> managerPolicyElements;
	private String tag;

	public SecurityGroupInterfaceElementImpl(String managerSecurityGroupInterfaceId, String name,
			String managerSecurityGroupId, Set<ManagerPolicyElement> managerPolicyElements, String tag) {
		super();
		this.managerSecurityGroupInterfaceId = managerSecurityGroupInterfaceId;
		this.name = name;
		this.managerSecurityGroupId = managerSecurityGroupId;
		this.managerPolicyElements = managerPolicyElements;
		this.tag = tag;
	}

	@Override
	public String getManagerSecurityGroupInterfaceId() {
		return this.managerSecurityGroupInterfaceId;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getManagerSecurityGroupId() {
		return this.managerSecurityGroupId;
	}

	@Override
	public Set<ManagerPolicyElement> getManagerPolicyElements() {
		return this.managerPolicyElements;
	}

	@Override
	public String getTag() {
		return this.tag;
	}

}
