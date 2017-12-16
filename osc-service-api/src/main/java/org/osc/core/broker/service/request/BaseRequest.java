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

import javax.xml.bind.annotation.XmlElement;

import org.osc.core.broker.service.dto.BaseDto;

import io.swagger.annotations.ApiModelProperty;

/**
 * Default Implementation of a request. Any common information for requests can be added
 * in here to avoid code duplication.
 *
 * @param <T>
 *            the type of DTO object
 */
public class BaseRequest<T extends BaseDto> implements Request {

    private T dto;

    @XmlElement(name = "api")
    private boolean isApi;

    public BaseRequest() {
    }

    public BaseRequest(boolean isApi) {
        this.isApi = isApi;
    }

    public BaseRequest(T dto) {
        this.dto = dto;
    }

    public T getDto() {
        return this.dto;
    }

    public void setDto(T dto) {
        this.dto = dto;
    }

    // Make sure swagger ignores this property
    @ApiModelProperty(readOnly = true, hidden = true)
    public boolean isApi() {
        return this.isApi;
    }

    public void setApi(boolean isApi) {
        this.isApi = isApi;
    }

    @Override
    public String toString() {
        return "BaseRequest [dto=" + this.dto + ", isApi=" + this.isApi + "]";
    }
}
