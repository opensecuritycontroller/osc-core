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
package org.osc.core.broker.service.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModel;

@ApiModel(description="Parent Id is applicable for this object. The corresponding virtualization connector is considered.")
@XmlRootElement(name = "serviceFunctionChain")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceFunctionChainDto extends BaseDto {

    private String name;

	private List<VirtualSystemDto> virtualSystemDto;

    public ServiceFunctionChainDto() {
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<VirtualSystemDto> getVirtualSystemDto() {
		return this.virtualSystemDto;
	}

	public void setVirtualSystemDto(List<VirtualSystemDto> virtualSystemDto) {
		this.virtualSystemDto = virtualSystemDto;
	}

    @Override
    public String toString() {
        return "ServiceFunctionChainDto [name=" + this.name + ", virtualSystemDto=" + this.virtualSystemDto + "]";
    }
}
