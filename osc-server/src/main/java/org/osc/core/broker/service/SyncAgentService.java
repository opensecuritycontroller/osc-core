package org.osc.core.broker.service;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.request.DistributedApplianceInstancesRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.agent.SyncAgentMetaTask;
import org.osc.core.broker.util.ValidateUtil;

import com.mcafee.vmidc.server.Server;

public class SyncAgentService extends ServiceDispatcher<DistributedApplianceInstancesRequest, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(SyncAgentService.class);
    private List<DistributedApplianceInstance> daiList = null;

    @Override
    public BaseJobResponse exec(DistributedApplianceInstancesRequest request, Session session) throws Exception {
        DistributedApplianceInstancesRequest.checkForNullFields(request);

        BaseJobResponse response = new BaseJobResponse();
        this.daiList =  DistributedApplianceInstanceEntityMgr.getByIds(session, request.getDtoIdList());

        ValidateUtil.handleActionNotSupported(this.daiList);

        response.setJobId(startRegisterAgentJob());
        return response;
    }

    public Long startRegisterAgentJob() throws Exception {
        log.info("Syncing selected agents ");
        TaskGraph tg = new TaskGraph();
        tg.addTask(new SyncAgentMetaTask(this.daiList));
        Job job = JobEngine.getEngine().submit("Syncing "+ Server.PRODUCT_NAME + " Agent(s)", tg, null);
        log.info("Done submitting with jobId: " + job.getId());
        return job.getId();
    }

}
