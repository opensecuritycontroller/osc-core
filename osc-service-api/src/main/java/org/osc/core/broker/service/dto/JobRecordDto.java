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

import java.util.Date;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.job.LockObjectDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description="Parent Id is not applicable for this object")
@XmlRootElement(name="job")
@XmlAccessorType(XmlAccessType.FIELD)
public class JobRecordDto extends BaseDto {

    private String name;
    private String failureReason;
    private String state;
    private String status;
    private Date queued;
    private Date started;
    private Date completed;
    private Long taskCount;
    private Long taskCompleted;
    private String submittedBy;

    @ApiModelProperty(value = "List or object references relevant to this job. For example, in a Distributed Appliance "
            + "Synchronization Job, the Distributed Appliance Object reference will be included ")
    private Set<LockObjectDto> objects;

    @Override
    public String toString() {
        return "JobDto [id=" + getId() + ", name=" + this.name + ", state=" + this.state + ", status=" + this.status + ", queued=" + this.queued
                + ", started=" + this.started + ", completed=" + this.completed + "]";
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getQueued() {
        return this.queued;
    }

    public void setQueued(Date queued) {
        this.queued = queued;
    }

    public Date getStarted() {
        return this.started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getCompleted() {
        return this.completed;
    }

    public void setCompleted(Date completed) {
        this.completed = completed;
    }

    public Long getTaskCount() {
        return this.taskCount;
    }

    public void setTaskCount(Long taskCount) {
        this.taskCount = taskCount;
    }

    public Long getTaskCompleted() {
        return this.taskCompleted;
    }

    public void setTaskCompleted(Long taskCompleted) {
        this.taskCompleted = taskCompleted;
    }

    public String getFailureReason() {
        return this.failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Set<LockObjectDto> getObjects() {
        return this.objects;
    }

    public void setObjects(Set<LockObjectDto> objects) {
        this.objects = objects;
    }

    public String getSubmittedBy() {
        return this.submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

}
