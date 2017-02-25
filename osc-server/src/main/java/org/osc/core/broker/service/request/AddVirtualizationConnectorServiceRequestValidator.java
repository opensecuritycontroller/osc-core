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
package org.osc.core.broker.service.request;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.dto.DtoValidator;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.util.VirtualizationConnectorUtil;

public class AddVirtualizationConnectorServiceRequestValidator
		implements RequestValidator<DryRunRequest<VirtualizationConnectorDto>, VirtualizationConnector> {
	private Session session;
	private static final Logger LOG = Logger.getLogger(AddVirtualizationConnectorServiceRequestValidator.class);

	private DtoValidator<VirtualizationConnectorDto, VirtualizationConnector> dtoValidator;

	private VirtualizationConnectorUtil virtualizationConnectorUtil;

	public void setVirtualizationConnectorUtil(VirtualizationConnectorUtil virtualizationConnectorUtil) {
		this.virtualizationConnectorUtil = virtualizationConnectorUtil;
	}

	public AddVirtualizationConnectorServiceRequestValidator(Session session) {
		this.session = session;
	}

	@Override
	public void validate(DryRunRequest<VirtualizationConnectorDto> request) throws Exception {

		if (this.dtoValidator == null) {
			this.dtoValidator = new VirtualizationConnectorDtoValidator(session);
		}

		VirtualizationConnectorDto dto = request.getDto();
		dtoValidator.validateForCreate(dto);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(dto);

		if (virtualizationConnectorUtil == null) {
			virtualizationConnectorUtil = new VirtualizationConnectorUtil();
		}
		if (dto.getType().isVmware()) {
			virtualizationConnectorUtil.checkVmwareConnection(request, vc);
		} else {
			virtualizationConnectorUtil.checkOpenstackConnection(request, vc);
		}
	}

	@Override
	public VirtualizationConnector validateAndLoad(DryRunRequest<VirtualizationConnectorDto> request) throws Exception {
		throw new UnsupportedOperationException();
	}

}