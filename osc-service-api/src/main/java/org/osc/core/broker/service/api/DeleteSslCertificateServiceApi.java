package org.osc.core.broker.service.api;

import org.osc.core.broker.service.request.DeleteSslEntryRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public interface DeleteSslCertificateServiceApi
        extends ServiceDispatcherApi<DeleteSslEntryRequest, EmptySuccessResponse> {
}
