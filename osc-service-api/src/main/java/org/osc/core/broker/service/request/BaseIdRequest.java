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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.BaseDto;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseIdRequest extends BaseRequest<BaseDto> {
    private Long id;
    private Long parentId;

    public BaseIdRequest(long id, long parentId) {
        this.id = id;
        this.parentId = parentId;
    }

    public BaseIdRequest(long id) {
        this.id = id;
        this.parentId = null; // parent unassigned
    }

    public BaseIdRequest() {
        this.id = null; // id unassigned
        this.parentId = null; // parent unassigned
    }

    public Long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    // Make sure swagger hides the dto field when generating documentation
    @ApiModelProperty(hidden = true)
    @Override
    public BaseDto getDto() {
        throw new UnsupportedOperationException();
    }

    // Make sure swagger hides the dto field when generating documentation
    @ApiModelProperty(hidden = true)
    @Override
    public boolean isApi() {
        return super.isApi();
    }

}
