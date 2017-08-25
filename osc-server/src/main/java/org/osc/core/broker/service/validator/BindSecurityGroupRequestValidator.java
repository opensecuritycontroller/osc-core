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
package org.osc.core.broker.service.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.service.dto.VirtualSystemPolicyBindingDto;
import org.osc.core.broker.service.request.BindSecurityGroupRequest;
import org.osc.core.broker.util.ValidateUtil;

public class BindSecurityGroupRequestValidator {

	public static void checkForNullFields(BindSecurityGroupRequest request) throws Exception {

		// build a map of (field,value) pairs to be checked for null/empty
		// values
		Map<String, Object> map = new HashMap<>();

		map.put("Security Group Id", request.getSecurityGroupId());
		List<VirtualSystemPolicyBindingDto> services = request.getServicesToBindTo();
		if (services != null && !services.isEmpty()) {
			for (VirtualSystemPolicyBindingDto service : services) {
				if (service.getVirtualSystemId() == null || StringUtils.isBlank(service.getName())
						|| service.getOrder() == null) {
					map.put("Virtual System Id", service.getVirtualSystemId());
					map.put("Service Name", service.getName());
					map.put("Service Order", service.getOrder());
					break;
				}
			}
		}

		ValidateUtil.checkForNullFields(map);
	}

}
