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
package org.osc.core.broker.model.entities.job;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.common.job.JobState;
import org.osc.core.common.job.JobStatus;

@Entity
@Table(name = "JOB")
public class JobRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private JobState state;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "queued_timestamp")
    private Date queuedTimestamp;
    @Column(name = "started_timestamp")
    private Date startedTimestamp;
    @Column(name = "completed_timestamp")
    private Date completedTimestamp;

    @Column(name = "submitted_by")
    private String submittedBy;

    @OneToMany(targetEntity = TaskRecord.class, mappedBy = "job", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<TaskRecord> tasks = new HashSet<TaskRecord>();

    @OneToMany(targetEntity = JobObject.class, mappedBy = "job", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<JobObject> objects = new HashSet<JobObject>();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<TaskRecord> getTasks() {
        return this.tasks;
    }

    public JobState getState() {
        return this.state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    public JobStatus getStatus() {
        return this.status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Date getQueuedTimestamp() {
        return this.queuedTimestamp;
    }

    public void setQueuedTimestamp(Date queuedTimestamp) {
        this.queuedTimestamp = queuedTimestamp;
    }

    public Date getStartedTimestamp() {
        return this.startedTimestamp;
    }

    public void setStartedTimestamp(Date startedTimestamp) {
        this.startedTimestamp = startedTimestamp;
    }

    public Date getCompletedTimestamp() {
        return this.completedTimestamp;
    }

    public void setCompletedTimestamp(Date date) {
        this.completedTimestamp = date;
    }

    public String getSubmittedBy() {
        return this.submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public String getFailureReason() {
        return this.failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Set<JobObject> getObjects() {
        return this.objects;
    }

    public void addObject(JobObject object) {
        this.objects.add(object);
    }

    @Override
    public String toString() {
        return "JobRecord [state=" + this.state + ", status=" + this.status + ", getId()=" + getId() + ", getName()=" + getName()
        + "]";
    }

}