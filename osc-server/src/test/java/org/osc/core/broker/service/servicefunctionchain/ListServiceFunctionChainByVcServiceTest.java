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

package org.osc.core.broker.service.servicefunctionchain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.service.dto.ServiceFunctionChainDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

@RunWith(MockitoJUnitRunner.class)
public class ListServiceFunctionChainByVcServiceTest extends BaseServiceFunctionChainServiceTest {

	@InjectMocks
	private ListServiceFunctionChainByVcService service;

	private BaseIdRequest request;

	@Override
	@Before
	public void testInitialize() throws Exception {
		super.testInitialize();
		this.request = new BaseIdRequest();
		this.service.validator = this.validatorMock;
		Mockito.when(this.service.validator.create(this.em)).thenReturn(this.validatorMock);
	}

	@Test
	public void testDispatch_WithNullVcId_ThrowsIllegalArgumentException() throws Exception {
		// vcid is null value , if nothing is set.
		Mockito.when(this.service.validator.create(this.em).validateVirtualConnector(this.em, null)).thenCallRealMethod();
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage("id to load is required for loading");

		// Act.
		this.service.dispatch(this.request);
	}

	@Test
	public void testDispatch_WithInvalidVcId_ThrowsVmidcBrokerValidationException() throws Exception {

		Mockito.when(this.service.validator.validateVirtualConnector(this.em, 222L)).thenCallRealMethod();
		this.request.setId(222L);
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage("Virtualization Connector id " + this.request.getId() + " is not found.");

		// Act.
		this.service.dispatch(this.request);
	}


	@Test
	public void testDispatch_WhenRequestIsValid_ValidationSucceeds() throws Exception {
		Mockito.when(this.service.validator.validateVirtualConnector(this.em, this.vc.getId())).thenCallRealMethod();
		this.request.setId(this.vc.getId());
		// Act.
		ListResponse<ServiceFunctionChainDto> response = this.service.dispatch(this.request);
		Assert.assertNotNull("The returned response should not be null.", response);

	}

}