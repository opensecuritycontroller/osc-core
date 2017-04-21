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
package org.osc.core.broker.service.dto.job;

import org.osc.core.broker.service.dto.BaseDto;

import io.swagger.annotations.ApiModelProperty;

public class LockObjectDto extends BaseDto {

    @ApiModelProperty(required = true)
    private ObjectTypeDto type;
    @ApiModelProperty(required = true)
    private String name;

    public LockObjectDto() {
    }

    public LockObjectDto(Long id, String name, ObjectTypeDto type) {
        setId(id);
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectTypeDto getType() {
        return this.type;
    }

    public void setType(ObjectTypeDto type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "LockObjectDto [name=" + this.name + ", type=" + this.type + ", id=" + getId() +"]";
    }

}
