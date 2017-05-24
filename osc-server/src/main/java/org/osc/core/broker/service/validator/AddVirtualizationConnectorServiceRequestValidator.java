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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.VirtualizationConnectorUtil;

public class AddVirtualizationConnectorServiceRequestValidator
		implements RequestValidator<DryRunRequest<VirtualizationConnectorRequest>, VirtualizationConnector> {
	private EntityManager em;

	private DtoValidator<VirtualizationConnectorDto, VirtualizationConnector> dtoValidator;

	private VirtualizationConnectorUtil virtualizationConnectorUtil;

    private TransactionalBroadcastUtil txBroadcastUtil;

	public void setVirtualizationConnectorUtil(VirtualizationConnectorUtil virtualizationConnectorUtil) {
		this.virtualizationConnectorUtil = virtualizationConnectorUtil;
	}

	public AddVirtualizationConnectorServiceRequestValidator(EntityManager em, TransactionalBroadcastUtil txBroadcastUtil) {
		this.em = em;
        this.txBroadcastUtil = txBroadcastUtil;
	}

	@Override
	public void validate(DryRunRequest<VirtualizationConnectorRequest> request) throws Exception {

		if (this.dtoValidator == null) {
			this.dtoValidator = new VirtualizationConnectorDtoValidator(this.em, this.txBroadcastUtil);
		}

		VirtualizationConnectorDto dto = request.getDto();
		this.dtoValidator.validateForCreate(dto);

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.createEntity(dto,
		        StaticRegistry.encryptionApi());

		if (this.virtualizationConnectorUtil == null) {
			this.virtualizationConnectorUtil = new VirtualizationConnectorUtil();
		}
		if (dto.getType().isVmware()) {
			this.virtualizationConnectorUtil.checkVmwareConnection(request, vc);
		} else {
			this.virtualizationConnectorUtil.checkOpenstackConnection(request, vc);
		}
	}

	@Override
	public VirtualizationConnector validateAndLoad(DryRunRequest<VirtualizationConnectorRequest> request) throws Exception {
		throw new UnsupportedOperationException();
	}

}