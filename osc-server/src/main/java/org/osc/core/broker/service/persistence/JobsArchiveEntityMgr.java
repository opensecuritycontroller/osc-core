package org.osc.core.broker.service.persistence;

import org.osc.core.broker.model.entities.archive.JobsArchive;
import org.osc.core.broker.service.archive.JobsArchiveDto;

public class JobsArchiveEntityMgr {

    public static JobsArchive createEntity(JobsArchiveDto dto) {
    	JobsArchive jobsArchive = new JobsArchive();
        toEntity(jobsArchive, dto);
        return jobsArchive;
    }

    public static void toEntity(JobsArchive jobsArchive, JobsArchiveDto dto) {

        // transform from dto to entity
        jobsArchive.setId(dto.getId());
        jobsArchive.setAutoSchedule(dto.getAutoSchedule());
        jobsArchive.setFrequency(dto.getFrequency());
        jobsArchive.setThresholdUnit(dto.getThresholdUnit());
        jobsArchive.setThresholdValue(dto.getThresholdValue());
        jobsArchive.setLastTriggerTimestamp(dto.getLastTriggerTimestamp());
    }

    public static void fromEntity(JobsArchive jobsArchive, JobsArchiveDto dto) {
        dto.setId(jobsArchive.getId());
        dto.setAutoSchedule(jobsArchive.getAutoSchedule());
        dto.setFrequency(jobsArchive.getFrequency());
        dto.setThresholdUnit(jobsArchive.getThresholdUnit());
        dto.setThresholdValue(jobsArchive.getThresholdValue());
        dto.setLastTriggerTimestamp(jobsArchive.getLastTriggerTimestamp());
    }
}
