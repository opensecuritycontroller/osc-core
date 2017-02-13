package org.osc.core.broker.di;

import org.osc.core.broker.service.DownloadAgentLogService;
import org.osc.core.broker.service.GetAgentStatusService;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.ListDistributedApplianceInstanceService;
import org.osc.core.broker.service.ListJobService;
import org.osc.core.broker.service.RegisterAgentService;
import org.osc.core.broker.service.SyncAgentService;
import org.osc.core.broker.service.alert.AcknowledgeAlertService;
import org.osc.core.broker.service.alert.DeleteAlertService;
import org.osc.core.broker.service.alert.ListAlertService;
import org.osc.core.broker.util.api.ApiUtil;
import org.osc.core.broker.util.api.ApiUtilImpl;
import org.osc.core.broker.util.session.SessionUtil;
import org.osc.core.broker.util.session.SessionUtilImpl;

public class OSCProductionFactory implements OSCFactory {
    private SessionUtil sessionUtil;
    private ApiUtil apiUtil;

    public OSCProductionFactory() {
        sessionUtil = new SessionUtilImpl();
        apiUtil = new ApiUtilImpl();
    }

    @Override
    public SessionUtil sessionUtil() {
        return sessionUtil;
    }

    @Override
    public ApiUtil apiUtil() {
        return apiUtil;
    }

    @Override
    public ListJobService listJobService() {
        return new ListJobService();
    }

    @Override
    public ListAlertService listAlertService() {
        return new ListAlertService();
    }

    @Override
    public AcknowledgeAlertService acknowledgeAlertService() {
        return new AcknowledgeAlertService();
    }

    @Override
    public DeleteAlertService deleteAlertService() {
        return new DeleteAlertService();
    }

    @Override
    public GetDtoFromEntityService dtoFromEntityService() {
        return new GetDtoFromEntityService();
    }

    @Override
    public ListDistributedApplianceInstanceService listDistributedApplianceInstanceService() {
        return new ListDistributedApplianceInstanceService();
    }

    @Override
    public DownloadAgentLogService downloadAgentLogService() {
        return new DownloadAgentLogService();
    }

    @Override
    public RegisterAgentService registerAgentService() {
        return new RegisterAgentService();
    }

    @Override
    public SyncAgentService syncAgentService() {
        return new SyncAgentService();
    }

    @Override
    public GetAgentStatusService getAgentStatusService() {
        return new GetAgentStatusService();
    }
}
