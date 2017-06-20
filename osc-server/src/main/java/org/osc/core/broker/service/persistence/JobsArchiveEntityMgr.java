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
package org.osc.core.broker.service.persistence;

import org.osc.core.broker.model.entities.archive.JobsArchive;
import org.osc.core.broker.service.dto.JobsArchiveDto;
import org.osc.core.common.job.FreqType;
import org.osc.core.common.job.ThresholdType;

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
        jobsArchive.setFrequency(FreqType.valueOf(dto.getFrequency()));
        jobsArchive.setThresholdUnit(ThresholdType.valueOf(dto.getThresholdUnit()));
        jobsArchive.setThresholdValue(dto.getThresholdValue());
        jobsArchive.setLastTriggerTimestamp(dto.getLastTriggerTimestamp());
    }

    public static void fromEntity(JobsArchive jobsArchive, JobsArchiveDto dto) {
        dto.setId(jobsArchive.getId());
        dto.setAutoSchedule(jobsArchive.getAutoSchedule());
        dto.setFrequency(jobsArchive.getFrequency().toString());
        dto.setThresholdUnit(jobsArchive.getThresholdUnit().toString());
        dto.setThresholdValue(jobsArchive.getThresholdValue());
        dto.setLastTriggerTimestamp(jobsArchive.getLastTriggerTimestamp());
    }
}
