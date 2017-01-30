package org.osc.core.agent.server;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class RegisterJob implements Job {

    public RegisterJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Register.registerAppliance(false);
    }

}
