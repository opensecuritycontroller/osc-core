package org.osc.core.broker.di;

import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.ListJobService;
import org.osc.core.broker.util.api.ApiUtil;
import org.osc.core.broker.util.api.ApiUtilImpl;
import org.osc.core.broker.util.session.SessionUtil;
import org.osc.core.broker.util.session.SessionUtilTestImpl;

import static org.mockito.Mockito.mock;

public class OSCTestFactory implements OSCFactory {
    private SessionUtil sessionUtil;
    private ApiUtil apiUtil;

    private ListJobService listJobService;
    private GetDtoFromEntityService dtoFromEntityService;

    public OSCTestFactory() {
        sessionUtil = new SessionUtilTestImpl();
        apiUtil = mock(ApiUtilImpl.class);

        listJobService = mock(ListJobService.class);
        dtoFromEntityService = mock(GetDtoFromEntityService.class);
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
    public GetDtoFromEntityService dtoFromEntityService() {
        return dtoFromEntityService;
    }
}
