package org.osc.core.broker.service.dto;

import java.io.Serializable;

public class TaskFailureRecordDto implements Serializable {

    private static final long serialVersionUID = -9045645486832978243L;

    private String taskFailureReason;
    private long taskFailureCount;

    public TaskFailureRecordDto(String taskFailureReason, long taskFailureCount) {
        this.taskFailureReason = taskFailureReason;
        this.taskFailureCount = taskFailureCount;
    }

    public String getTaskFailureReason() {
        return this.taskFailureReason;
    }

    public long getTaskFailureCount() {
        return this.taskFailureCount;
    }

}