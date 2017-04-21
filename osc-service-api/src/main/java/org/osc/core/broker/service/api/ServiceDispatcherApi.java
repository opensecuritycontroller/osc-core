package org.osc.core.broker.service.api;

import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;

public interface ServiceDispatcherApi<I extends Request, O extends Response> {
    O dispatch(I request) throws Exception;
}
