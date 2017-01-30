package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.request.ListRequestValidator;
import org.osc.core.broker.service.request.UpdateDaiConsolePasswordRequest;
import org.osc.core.broker.service.request.UpdateDaiConsolePasswordRequestValidator;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.agent.UpdateApplianceConsolePasswordsMetaTask;

import java.util.List;

public class UpdateApplianceConsolePasswordService extends ServiceDispatcher<UpdateDaiConsolePasswordRequest, BaseJobResponse> {

    private ListRequestValidator<UpdateDaiConsolePasswordRequest, DistributedApplianceInstance> validator;

    @Override
    public BaseJobResponse exec(UpdateDaiConsolePasswordRequest request, Session session) throws Exception {
        if (this.validator == null) {
            this.validator = new UpdateDaiConsolePasswordRequestValidator(session);
        }

        BaseJobResponse response = new BaseJobResponse();
        List<DistributedApplianceInstance> daiList = validator.validateAndLoadList(request);

        Long jobId = startUpdateApplianceConsolePassword(request, daiList);
        response.setJobId(jobId);

        return response;
    }

    private Long startUpdateApplianceConsolePassword(UpdateDaiConsolePasswordRequest req,
            List<DistributedApplianceInstance> daiList) throws Exception {

        TaskGraph tg = new TaskGraph();
        tg.addTask(new UpdateApplianceConsolePasswordsMetaTask(req.getNewPassword(), daiList));

        Job job = JobEngine.getEngine().submit(
                "Update Appliance(s) console password for Virtual System: '" + req.getVsName() + "'", tg, null);

        return job.getId();
    }

}
