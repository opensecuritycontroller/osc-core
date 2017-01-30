package org.osc.core.broker.service;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.DownloadAgentLogResponse;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;

public class DownloadAgentLogService extends ServiceDispatcher<BaseIdRequest, DownloadAgentLogResponse> {

    private static final Logger log = Logger.getLogger(DownloadAgentLogService.class);

    @Override
    public DownloadAgentLogResponse exec(BaseIdRequest request, Session session) throws Exception {

        BaseIdRequest.checkForNullId(request);

        DownloadAgentLogResponse response = new DownloadAgentLogResponse();

        List<DistributedApplianceInstance> dais = DistributedApplianceInstanceEntityMgr.getByIds(session,
                Arrays.asList(request.getId()));

        ValidateUtil.handleActionNotSupported(dais);

        // Since Id is not null and we found a dai(Since DistributedApplianceInstanceEntityMgr didnt throw an exception), we expect to have
        // one item in the list
        response.setSupportBundle(downloadLogFile(dais.get(0)));
        return response;
    }

    private File downloadLogFile(DistributedApplianceInstance dai) {
        try {
            VmidcAgentApi agentApi = new VmidcAgentApi(dai.getIpAddress(), 8090, AgentAuthFilter.VMIDC_AGENT_LOGIN,
                    AgentAuthFilter.VMIDC_AGENT_PASS);
            return agentApi.downloadLogFile();
        } catch (Exception e) {
            log.error("Failed to get Support Bundle from" + dai.getName());
        }
        return null;
    }

}
