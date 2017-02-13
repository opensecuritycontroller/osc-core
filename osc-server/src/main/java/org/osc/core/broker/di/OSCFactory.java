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
import org.osc.core.broker.util.session.SessionUtil;

public interface OSCFactory {

    SessionUtil sessionUtil();

    ApiUtil apiUtil();

    ListJobService listJobService();

    ListAlertService listAlertService();

    AcknowledgeAlertService acknowledgeAlertService();

    DeleteAlertService deleteAlertService();

    GetDtoFromEntityService dtoFromEntityService();

    ListDistributedApplianceInstanceService listDistributedApplianceInstanceService();

    DownloadAgentLogService downloadAgentLogService();

    RegisterAgentService registerAgentService();

    SyncAgentService syncAgentService();

    GetAgentStatusService getAgentStatusService();
}
