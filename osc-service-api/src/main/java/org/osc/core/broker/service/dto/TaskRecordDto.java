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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.service.dto.job.TaskState;
import org.osc.core.broker.service.dto.job.TaskStatus;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(
        description = "Parent Id is applicable for this object. The corresponding Job is considered the parent of this Task.")
@XmlRootElement(name = "task")
@XmlAccessorType(XmlAccessType.FIELD)
public class TaskRecordDto extends BaseDto {

    private Long dependencyOrder;
    private String name;
    private TaskState state;
    private TaskStatus status;
    private Date queued;
    private Date started;
    private Date completed;
    @ApiModelProperty(value="A comma seperated list of predecessor Task IDs for this Task")
    private String predecessors;
    private String failReason;

    @ApiModelProperty(value = "List of object references relevant to this job. For example, in a Distributed Appliance "
            + "Synchronization Job, the Distributed Appliance Object reference will be included ")
    private Set<LockObjectReference> objects;

    @Override
    public String toString() {
        return "TaskRecordDto [id=" + getId() + ", dependencyOrder=" + this.dependencyOrder + ", jobId=" + getParentId() + ", name="
                + this.name + ", state=" + this.state + ", status=" + this.status + ", queued=" + this.queued + ", started=" + this.started
                + ", completed=" + this.completed + ", failReason=" + this.failReason + "]";
    }

    public String getFailReason() {
        return this.failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public Long getDependencyOrder() {
        return this.dependencyOrder;
    }

    public void setDependencyOrder(Long dependency) {
        this.dependencyOrder = dependency;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TaskState getState() {
        return this.state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public TaskStatus getStatus() {
        return this.status;
    }

    public void setStatus(TaskStatus status) {
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

    public String getPredecessors() {
        return this.predecessors;
    }

    public void setPredecessors(String predecessors) {
        this.predecessors = predecessors;
    }

    public Set<LockObjectReference> getObjects() {
        return this.objects;
    }

    public void setObjects(Set<LockObjectReference> objects) {
        this.objects = objects;
    }
}
