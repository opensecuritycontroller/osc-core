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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.BaseDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description="Parent Id is applicable for this object. The corresponding virtualization connector is considered"
        + " the parent of this Service Function Chain.")
@XmlRootElement(name = "ServiceFunctionChain")
@XmlAccessorType(XmlAccessType.FIELD)
public class AddOrUpdateServiceFunctionChainRequest extends BaseRequest<BaseDto> {

	@ApiModelProperty(required = true)
	private String  name;

	@ApiModelProperty(required = false)
	private List<Long> virtualSystemIds;


	public AddOrUpdateServiceFunctionChainRequest() {
		super();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Long> getVirtualSystemIds() {
		return this.virtualSystemIds;
	}

	public void setVirtualSystemIds(List<Long> virtualSystemIds) {
		this.virtualSystemIds = virtualSystemIds;
	}

    // Make sure swagger hides the dto field when generating documentation
    @ApiModelProperty(hidden = true)
    @Override
    public BaseDto getDto() {
        return super.getDto();
    }


}
