package org.osc.core.broker.service.api;

import org.osc.core.broker.service.request.AddSslEntryRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public interface AddSslCertificateServiceApi
        extends ServiceDispatcherApi<AddSslEntryRequest, EmptySuccessResponse> {
}
