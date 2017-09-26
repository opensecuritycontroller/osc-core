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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.AddOrUpdateServiceFunctionChainRequest;
import org.osc.core.broker.service.response.BaseJobResponse;

@RunWith(MockitoJUnitRunner.class)
public class UpdateServiceFunctionChainServiceTest extends BaseServiceFunctionChainServiceTest {
	@InjectMocks
	private UpdateServiceFunctionChainService service;
	
	AddOrUpdateServiceFunctionChainRequest request;
	
	@Override
	@Before
	public void testInitialize() throws Exception {
		super.testInitialize();
		this.service.validator = this.validatorMock;
		request = new AddOrUpdateServiceFunctionChainRequest();
	}

	@Test
	public void testDispatch_WithNullRequest_ThrowsNullPointerException() throws Exception {

		// Arrange.
		this.exception.expect(NullPointerException.class);
		// Act.
		this.service.dispatch(null);
	}

	@Test
	public void testDispatch_WithNonEmptyListOfVsIdsUpdateValidationSuccessful_ValidationSucceeds() throws Exception {
		
		BaseDto dto = new BaseDto();
		dto.setId(this.sfc.getId());
		dto.setParentId(this.vc.getId());
		request.setDto(dto);
		request.setName(this.sfc.getName());
		List<Long> vsIds = new ArrayList<Long>();
		vsIds.add(this.vs.getId());
		vsIds.add(this.vs1.getId());
		request.setVirtualSystemIds(vsIds);
		Mockito.when(this.service.validator.validateAndLoad(request)).thenReturn(this.sfc);
		// Act.
		BaseJobResponse response = this.service.dispatch(request);
		Assert.assertNotNull("The returned response should not be null.", response);
	}
	
	@Test
	public void testDispatch_WhenVsIdsListUpdateValidationSuccessful_ValidationSucceeds() throws Exception {
		
		BaseDto dto = new BaseDto();
		dto.setId(this.sfc.getId());
		dto.setParentId(this.vc.getId());
		request.setDto(dto);
		request.setName(this.sfc.getName());
		List<Long> vsIds = new ArrayList<Long>();
		vsIds.add(this.vs1.getId());
		vsIds.add(this.vs.getId());
		request.setVirtualSystemIds(vsIds);
		Mockito.when(this.service.validator.validateAndLoad(request)).thenReturn(this.sfc);
		// Act.
		BaseJobResponse response = this.service.dispatch(request);
		Assert.assertNotNull("The returned response should not be null.", response);
	}
	
	//TODO : karimull  Add more test cases in looking for updated in order. Need some ramp up here
}
