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
@Table(name = "JOB_OBJECT", uniqueConstraints = { @UniqueConstraint(columnNames = { "job_fk", "object_type",
        "object_id" }) })
public class JobObject extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_fk", nullable = false)
    @ForeignKey(name = "FK_JOB_OBJECT_TASK")
    private JobRecord job;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "object_type")
    @Enumerated(EnumType.STRING)
    private ObjectType objectType;

    @Column(name = "object_id", nullable = false)
    private Long objectId;

    public JobObject() {
    }

    public JobObject(JobRecord job, LockObjectReference lor) {
        this(job, lor.getName(), lor.getType(), lor.getId());
    }

    public JobObject(JobRecord job, String name, ObjectType objectType, Long objectId) {
        this.job = job;
        job.addObject(this);

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

    public JobRecord getJob() {
        return job;
    }

    @Override
    public String toString() {
        return "TaskObject [objectType=" + objectType + ", name=" + name + ", objectId=" + objectId + "]";
    }

}
