package org.osc.core.broker.model.entities.job;

import org.osc.core.broker.model.entities.IscEntity;

/**
 * Attaches to entity info about last job
 */
public interface LastJobContainer extends IscEntity{
    void setLastJob(JobRecord lastJob);
    JobRecord getLastJob();
}
