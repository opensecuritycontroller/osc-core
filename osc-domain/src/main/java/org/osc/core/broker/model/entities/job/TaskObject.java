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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.ObjectType;

@Entity
@Table(name = "TASK_OBJECT", uniqueConstraints = { @UniqueConstraint(columnNames = { "task_fk", "object_type",
        "object_id" }) })
public class TaskObject extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_fk", nullable = false)
    @ForeignKey(name = "FK_TASK_OBJECT_TASK")
    private TaskRecord task;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "object_type")
    @Enumerated(EnumType.STRING)
    private ObjectType objectType;

    @Column(name = "object_id", nullable = false)
    private Long objectId;

    public TaskObject() {
    }

    public TaskObject(TaskRecord task, String name, ObjectType objectType, Long objectId) {
        this.task = task;
        task.addObject(this);

        this.name = name;
        this.objectType = objectType;
        this.objectId = objectId;
    }

    public ObjectType getObjectType() {
        return this.objectType;
    }

    public Long getObjectId() {
        return this.objectId;
    }

    public String getName() {
        return this.name;
    }

    public TaskRecord getTask() {
        return this.task;
    }

    @Override
    public String toString() {
        return "TaskObject [objectType=" + this.objectType + ", name=" + this.name + ", objectId=" + this.objectId + "]";
    }

}