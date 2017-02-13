package org.osc.core.broker.di;

import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.ListDistributedApplianceInstanceService;
import org.osc.core.broker.service.ListJobService;
import org.osc.core.broker.service.alert.AcknowledgeAlertService;
import org.osc.core.broker.service.alert.DeleteAlertService;
import org.osc.core.broker.service.alert.ListAlertService;
import org.osc.core.broker.util.api.ApiUtil;
import org.osc.core.broker.util.api.ApiUtilImpl;
import org.osc.core.broker.util.session.SessionUtil;
import org.osc.core.broker.util.session.SessionUtilTestImpl;

import static org.mockito.Mockito.mock;

public class OSCTestFactory implements OSCFactory {
    private SessionUtil sessionUtil;
    private ApiUtil apiUtil;

    private ListJobService listJobService;
    private ListAlertService listAlertService;
    private AcknowledgeAlertService acknowledgeAlertService;
    private DeleteAlertService deleteAlertService;
    private GetDtoFromEntityService dtoFromEntityService;
    private ListDistributedApplianceInstanceService listDistributedApplianceInstanceService;

    public OSCTestFactory() {
        sessionUtil = new SessionUtilTestImpl();
        apiUtil = mock(ApiUtilImpl.class);

        listJobService = mock(ListJobService.class);

        listAlertService = mock(ListAlertService.class);
        acknowledgeAlertService = mock(AcknowledgeAlertService.class);
        deleteAlertService = mock(DeleteAlertService.class);

        dtoFromEntityService = mock(GetDtoFromEntityService.class);

        listDistributedApplianceInstanceService = mock(ListDistributedApplianceInstanceService.class);
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
        return listJobService;
    }

    @Override
    public ListAlertService listAlertService() {
        return listAlertService;
    }

    @Override
    public AcknowledgeAlertService acknowledgeAlertService() {
        return acknowledgeAlertService;
    }

    @Override
    public DeleteAlertService deleteAlertService() {
        return deleteAlertService;
    }

    @Override
    public GetDtoFromEntityService dtoFromEntityService() {
        return dtoFromEntityService;
    }

    @Override
    public ListDistributedApplianceInstanceService listDistributedApplianceInstanceService() {
        return listDistributedApplianceInstanceService;
    }
}
