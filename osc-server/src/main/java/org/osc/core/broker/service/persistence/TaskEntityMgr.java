package org.osc.core.broker.service.persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.job.TaskState;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.job.TaskObject;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.service.dto.TaskFailureRecordDto;
import org.osc.core.broker.service.dto.TaskRecordDto;

public class TaskEntityMgr extends EntityManager<TaskRecord> {

    public TaskEntityMgr(Session session) {
        super(TaskRecord.class, session);
    }

    public static TaskRecordDto fromEntity(TaskRecord tr) {
        TaskRecordDto taskDto = new TaskRecordDto();
        fromEntity(tr, taskDto);
        return taskDto;
    }

    public static void fromEntity(TaskRecord tr, TaskRecordDto taskDto) {
        taskDto.setId(tr.getId());
        taskDto.setParentId(tr.getJob().getId());
        taskDto.setName(tr.getName());
        taskDto.setStatus(tr.getStatus());
        taskDto.setState(tr.getState());
        taskDto.setQueued(tr.getQueuedTimestamp());
        taskDto.setStarted(tr.getStartedTimestamp());
        taskDto.setCompleted(tr.getCompletedTimestamp());
        taskDto.setDependencyOrder(tr.getDependencyOrder());
        taskDto.setFailReason(tr.getFailReason());
        taskDto.setPredecessors(tr.getPredecessorsOrderIds());
        taskDto.setObjects(getJobObjects(tr));
    }

    private static Set<LockObjectReference> getJobObjects(TaskRecord task) {
        if (task.getObjects() == null) {
            return null;
        }

        Set<LockObjectReference> objects = new HashSet<LockObjectReference>();
        for (TaskObject jo : task.getObjects()) {
            objects.add(new LockObjectReference(jo.getObjectId(), jo.getName(), jo.getObjectType()));
        }
        return objects;
    }

    @SuppressWarnings("unchecked")
    public List<TaskRecord> getTasksByJobId(Long jobId) {
        return this.session.createCriteria(TaskRecord.class).add(Restrictions.eq("job.id", jobId))
                .addOrder(Order.asc("dependencyOrder")).list();
    }

    /**
     * Gets all the unique Task failures encountered since the from date.
     *
     * @param fromDate the start time to look for failed task
     * @param session the hibernate session
     *
     * @return unique task failures since the from date
     */
    public static List<TaskFailureRecordDto> getUniqueTaskFailureStrings(Date fromDate, Session session) {

        List<TaskFailureRecordDto> taskFailures = null;

        Criterion restriction = null;

        if(fromDate != null) {
            restriction = Restrictions.and(Restrictions.gt("completedTimestamp", fromDate), Restrictions.isNotNull("failReason"));
        } else {
            restriction = Restrictions.isNotNull("failReason");
        }
        List<?> results = session
                .createCriteria(TaskRecord.class)
                .add(restriction)
                .setProjection(
                        Projections.projectionList().add(Projections.groupProperty("failReason"))
                                .add(Projections.rowCount())).list();
        if (results != null) {
            taskFailures = new ArrayList<TaskFailureRecordDto>();
            for (Object row : results) {
                Object[] rowArray = (Object[]) row;
                taskFailures.add(new TaskFailureRecordDto((String) rowArray[0], (long) rowArray[1]));
            }
        }

        return taskFailures;

    }

    @SuppressWarnings("unchecked")
    public List<TaskRecord> getUncompletedTasks() {
        return this.session.createCriteria(TaskRecord.class).add(Restrictions.ne("state", TaskState.COMPLETED)).list();
    }

}

