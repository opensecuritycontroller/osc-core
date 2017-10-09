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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
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

        this.request = new AddOrUpdateServiceFunctionChainRequest();

        BaseDto dto = new BaseDto();
        dto.setId(this.sfc.getId());
        dto.setParentId(this.vc.getId());
        this.request.setDto(dto);
        this.request.setName(this.sfc.getName());

        Mockito.when(this.service.validator.validateAndLoad(this.request)).thenReturn(this.sfc);
    }

	@Test
	public void testDispatch_WhenVirtualSystemListEmpty_SfcIsUpdated() throws Exception {

		// Act.
		BaseJobResponse response = this.service.dispatch(this.request);
		List<Long> sfcVsIdList = this.em.find(ServiceFunctionChain.class, this.sfc.getId()).getVirtualSystems().stream()
                .map(vss -> vss.getId()).collect(Collectors.toList());

		// Assert
		Assert.assertEquals("The list of virtual system ids is not Empty", Collections.EMPTY_LIST, sfcVsIdList);
		Assert.assertNotNull("The returned response should not be null.", response);
		Assert.assertEquals("The return Id should be equal to sfc Id", response.getId(), this.sfc.getId());
	}


	@Test
	public void testDispatch_WhenVirtualSystemListNotEmpty_SfcIsUpdatedAndListInOrder() throws Exception {

		// Arrange
		List<Long> vsIds = new ArrayList<>();

		vsIds.add(this.vs1.getId());
		vsIds.add(this.vs.getId());
		this.request.setVirtualSystemIds(vsIds);

		// Act.
		// update sfc with virtual system ids list {2,1}
		BaseJobResponse response = this.service.dispatch(this.request);

		// check Virtual system is as updated
		List<Long> sfcVsIdList = this.em.find(ServiceFunctionChain.class, this.sfc.getId()).getVirtualSystems().stream()
				.map(vss -> vss.getId()).collect(Collectors.toList());
		// read back sfc and cross check with local Virtual system list for
		// order

		// Assert
		Assert.assertEquals("The list of virtual system ids is different than expected", vsIds, sfcVsIdList);
		Assert.assertNotNull("The returned response should not be null.", response);
		Assert.assertEquals("The return Id should be equal to sfc Id", response.getId(), this.sfc.getId());
	}
}
