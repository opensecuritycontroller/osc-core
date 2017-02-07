package org.osc.core.broker.di;

import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.ListJobService;
import org.osc.core.broker.util.api.ApiUtil;
import org.osc.core.broker.util.session.SessionUtil;

public interface OSCFactory {
    SessionUtil sessionUtil();
    ApiUtil apiUtil();

    ListJobService listJobService();

    GetDtoFromEntityService dtoFromEntityService();
}
