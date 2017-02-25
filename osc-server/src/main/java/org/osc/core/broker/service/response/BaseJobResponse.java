/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.broker.service.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

/**
 * A basic Response object which contains a job id in addition to any properties inherited from
 * {@link BaseResponse}
 */

@XmlRootElement(name ="jobResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseJobResponse extends BaseResponse {

    @ApiModelProperty(value = "Id of the Job started", readOnly = true)
    private Long jobId;

    public BaseJobResponse() {
    }

    public BaseJobResponse(Long jobId) {
        this.jobId = jobId;
    }

    public BaseJobResponse(Long objectId, Long jobId) {
        super(objectId);
        this.jobId = jobId;
    }

    public Long getJobId() {
        return this.jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
}
