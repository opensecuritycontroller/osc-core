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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.AddOrUpdateServiceFunctionChainRequest;

@RunWith(MockitoJUnitRunner.class)
public class ServiceFunctionChainRequestValidatorTest extends BaseServiceFunctionChainRequestValidatorTest {

	@Test
	public void testValidate_ThrowsNullPointerException() throws Exception {

		// Arrange.
		this.exception.expect(NullPointerException.class);

		// Act.
		this.validator.validate(null);
	}

	@Test
	public void testValidate_WithNullNameField_ThrowsVmidcBrokerInvalidEntryException() throws Exception {

		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setDto(dto);
		// Arrange.
		this.exception.expect(VmidcBrokerInvalidEntryException.class);
		this.exception.expectMessage("Service Function Chain Name" + " should not have an empty value.");

		// Act.
		this.validator.validate(request);
	}


	@Test
	public void testValidate_WithInvalidNameLenghtMax_ThrowsVmidcBrokerValidationException() throws Exception {

		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		String name = StringUtils.repeat("s", 15);
		request.setName(name);
		dto.setParentId(1L);
		request.setDto(dto);
		// Arrange.
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage("Invalid Service Function Name: " + request.getName()
		+ "SFC name must not exceed 13 characters, must start with a letter, and can only contain numbers, letters and dash(-).");

		// Act.
		this.validator.validate(request);
	}

	@Test
	public void testValidate_WithBeginingInvalidNameChar_ThrowsVmidcBrokerValidationException() throws Exception {

		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setName("1sfc");
		dto.setParentId(1L);
		request.setDto(dto);
		// Arrange.
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage("Invalid Service Function Name: " + request.getName()
		+ "SFC name must not exceed 13 characters, must start with a letter, and can only contain numbers, letters and dash(-).");

		// Act.
		this.validator.validate(request);
	}

	@Test
	public void testValidate_WithInvalidNameChar_ThrowsVmidcBrokerValidationException() throws Exception {

		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setName("sfc**2");
		dto.setParentId(1L);
		request.setDto(dto);
		// Arrange.
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage("Invalid Service Function Name: " + request.getName()
		+ "SFC name must not exceed 13 characters, must start with a letter, and can only contain numbers, letters and dash(-).");
		

		// Act.
		this.validator.validate(request);
	}

	@Test
	public void testValidate_WhenParentIdIsNull_ThrowsVmidcBrokerValidationException() throws Exception {
		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setName("sfc-1");
		request.setDto(dto);
		// Arrange.
		this.exception.expect(VmidcBrokerInvalidEntryException.class);
		this.exception.expectMessage("Virtualization Connector Id should not have an empty value.");

		// Act.
		this.validator.validate(request);
	}

	@Test
	public void testValidate_WhenVcIdIsNotMatched_ThrowsVmidcBrokerValidationException() throws Exception {

		registerVirtualConnector("NONE", 1L);
		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setName("sfc-1");
		dto.setParentId(2L);
		request.setDto(dto);
		// Arrange.
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage("Virtualization Connector id " + dto.getParentId() + " is not found.");

		// Act.
		this.validator.validate(request);
	}

	@Test
	public void testValidate_WhenControllerTypeNotNone_ThrowsVmidcBrokerValidationException() throws Exception {

		VirtualizationConnector vc = registerVirtualConnector("OTHER", 1L);
		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setName("sfc-1");
		dto.setParentId(1L);
		request.setDto(dto);
		// Arrange.
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage("Creation of Service Function Chain is not allowed with controller of type " + vc.getControllerType());

		// Act.
		this.validator.validate(request);
	}

	@Test
	public void testValidate_WhenVsIdsNotExist_ThrowsVmidcBrokerValidationException() throws Exception {

		registerVirtualConnector("Neutron-sfc", 1L);
		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setName("sfc-1");
		dto.setParentId(1L);
		request.setDto(dto);
		List<Long> vsIds = new ArrayList<Long>();
		vsIds.add(1L);
		request.setVirtualSystemIds(vsIds);
		// Arrange.
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage("VirtualSytem with id " + 1 + " is not found.");

		// Act.
		this.validator.validate(request);
	}

	@Test
	public void testValidate_WhenSomeVsIdsNotExist_ThrowsVmidcBrokerValidationException() throws Exception {

		VirtualizationConnector vc = registerVirtualConnector("Neutron-sfc", 1L);
		Set<VirtualSystem> vsSet = new HashSet<>();
		vsSet.add(registerVirtualSystem(1L, vc));
		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setName("sfc-1");
		dto.setParentId(1L);
		request.setDto(dto);
		List<Long> vsIds = new ArrayList<Long>();
		vsIds.add(1L);
		vsIds.add(2L);
		request.setVirtualSystemIds(vsIds);
		// Arrange.
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage(String.format(
                "Virtual system with id %s and with Virtualization Connector id %s does not match the virtualization connector"
                        + " under which this Service function chain is being created/updated",
                1, vc.getId()));

		// Act.
		this.validator.validate(request);
	}

	@Test
	public void testValidate_WhenVcIdAndVsIdsVcIdNotMatch_ThrowsVmidcBrokerValidationException() throws Exception {

		registerVirtualConnector("Neutron-sfc", 1L);
		VirtualizationConnector vc = newVirtualConnector("Neutron-sfc");
		vc.setId(2L);
		registerVirtualSystem(1L, vc);
		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setName("sfc-1");
		dto.setParentId(1L);
		request.setDto(dto);
		List<Long> vsIds = new ArrayList<Long>();
		vsIds.add(1L);
		request.setVirtualSystemIds(vsIds);
		// Arrange.
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage(String.format(
                "Virtual system with id %s and with Virtualization Connector id %s does not match the virtualization connector"
                        + " under which this Service function chain is being created/updated",
                1, vc.getId()));

		// Act.
		this.validator.validate(request);
	}

	@Test
	public void testValidateAndLoad_WithNullIds_ThrowsVmidcBrokerInvalidEntryException() throws Exception {

		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setDto(dto);
		// Arrange.
		this.exception.expect(VmidcBrokerInvalidEntryException.class);
		this.exception.expectMessage("Service Function Chain Name" + " should not have an empty value.");

		// Act.
		this.validator.validateAndLoad(request);
	}

	@Test
	public void testValidateAndLoad_WhenFailedToFindEntity_ThrowsVmidcBrokerValidationException() throws Exception {

		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		dto.setId(1L);
		dto.setParentId(1L);
		request.setDto(dto);
		request.setName("some-name");
		// Arrange.
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage("Service Function Chain entry with name: " + request.getName() + " is not found.");

		// Act.
		this.validator.validateAndLoad(request);
	}

	@Test
	public void testValidateAndLoad_WhenMarkedForDeletion_ThrowsVmidcBrokerInvalidRequestException() throws Exception {

		VirtualizationConnector vc = registerVirtualConnector("NONE", 1L);
		ServiceFunctionChain sfc = registerServiceFunctionChain("sfc-1", 1L, vc, null);
		sfc.setMarkedForDeletion(true);
		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		dto.setId(1L);
		dto.setParentId(1L);
		request.setDto(dto);
		request.setName(sfc.getName());
		// Arrange.
		this.exception.expect(VmidcBrokerInvalidRequestException.class);
		this.exception.expectMessage("Invalid Request '" + sfc.getName() + "' is marked for deletion");

		// Act.
		this.validator.validateAndLoad(request);
	}
	
}
