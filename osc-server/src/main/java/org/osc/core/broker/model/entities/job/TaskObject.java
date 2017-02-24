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
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.model.entities.BaseEntity;

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

    public TaskObject(TaskRecord task, LockObjectReference lor) {
        this(task, lor.getName(), lor.getType(), lor.getId());
    }

    public TaskObject(TaskRecord task, String name, ObjectType objectType, Long objectId) {
        this.task = task;
        task.addObject(this);

        this.name = name;
        this.objectType = objectType;
        this.objectId = objectId;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public Long getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }

    public TaskRecord getTask() {
        return task;
    }

    @Override
    public String toString() {
        return "TaskObject [objectType=" + objectType + ", name=" + name + ", objectId=" + objectId + "]";
    }

}
