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
package org.osc.core.broker.model.entities.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;
import org.joda.time.DateTime;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.TaskState;
import org.osc.core.broker.job.TaskStatus;
import org.osc.core.broker.model.entities.BaseEntity;

@Entity
@Table(name = "TASK")
public class TaskRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_fk", nullable = false)
    @ForeignKey(name = "FK_TASK_JOB")
    private JobRecord job;

    @Column(name = "fail_reason", length = 1024)
    private String failReason;

    @Column(name = "task_gaurd", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskGuard taskGaurd;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private TaskState state;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "queued_timestamp")
    private Date queuedTimestamp;
    @Column(name = "started_timestamp")
    private Date startedTimestamp;
    @Column(name = "completed_timestamp")
    private Date completedTimestamp;

    @Column(name = "dependency_order", nullable = false)
    private Long dependencyOrder;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "TASK_PREDECESSOR", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "predecessor_id"))
    private Set<TaskRecord> predecessors = new HashSet<TaskRecord>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "TASK_SUCCESSOR", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "successor_id"))
    private Set<TaskRecord> successors = new HashSet<TaskRecord>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "TASK_CHILD", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "child_id"))
    private Set<TaskRecord> children = new HashSet<TaskRecord>();

    @OneToMany(targetEntity = TaskObject.class, mappedBy = "task", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<TaskObject> objects = new HashSet<TaskObject>();

    public TaskRecord() {
    }

    public TaskRecord(JobRecord jobRecord) {
        this.job = jobRecord;
    }

    public JobRecord getJob() {
        return job;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    void setJob(JobRecord job) {
        this.job = job;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        if (failReason.length() > 1024) {
            this.failReason = failReason.substring(0, 1023);
        } else {
            this.failReason = failReason;
        }
    }

    public TaskGuard getTaskGaurd() {
        return taskGaurd;
    }

    public void setTaskGaurd(TaskGuard taskGaurd) {
        this.taskGaurd = taskGaurd;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Date getQueuedTimestamp() {
        return queuedTimestamp;
    }

    public void setQueuedTimestamp(DateTime queuedTimestamp) {
        if (queuedTimestamp == null) {
            return;
        }
        this.queuedTimestamp = queuedTimestamp.toDate();
    }

    public Date getStartedTimestamp() {
        return startedTimestamp;
    }

    public void setStartedTimestamp(DateTime startedTimestamp) {
        if (startedTimestamp == null) {
            return;
        }
        this.startedTimestamp = startedTimestamp.toDate();
    }

    public Date getCompletedTimestamp() {
        return completedTimestamp;
    }

    public void setCompletedTimestamp(DateTime completedTimestamp) {
        if (completedTimestamp == null) {
            return;
        }
        this.completedTimestamp = completedTimestamp.toDate();
    }

    public Set<TaskRecord> getPredecessors() {
        return predecessors;
    }

    public Set<TaskRecord> getSuccessors() {
        return successors;
    }

    public Set<TaskRecord> getChildren() {
        return children;
    }

    public List<Long> getPredecessorsIds() {
        List<Long> ids = new ArrayList<Long>();
        for (TaskRecord tr : predecessors) {
            ids.add(tr.getId());
        }
        return ids;
    }

    public String getPredecessorsOrderIds() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (TaskRecord tr : predecessors) {
            sb.append(tr.getDependencyOrder() + ",");
        }
        if (!predecessors.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "TaskRecord [id=" + getId() + ", name=" + getName() + ", state=" + state + ", status=" + status
                + ", taskGaurd=" + taskGaurd + ", failReason=" + failReason + "]";
    }

    public void addSuccessor(TaskRecord task) {
        this.successors.add(task);
    }

    public void addPredecessor(TaskRecord task) {
        this.predecessors.add(task);
    }

    public void addChild(TaskRecord task) {
        this.children.add(task);
    }

    public Long getDependencyOrder() {
        return dependencyOrder;
    }

    public void setDependencyOrder(Long dependencyOrder) {
        this.dependencyOrder = dependencyOrder;
    }

    public Set<TaskObject> getObjects() {
        return this.objects;
    }

    public void addObject(TaskObject object) {
        this.objects.add(object);
    }

}
