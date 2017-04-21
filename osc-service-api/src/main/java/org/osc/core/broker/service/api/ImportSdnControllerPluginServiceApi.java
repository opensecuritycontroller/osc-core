package org.osc.core.broker.service.api;

import org.osc.core.broker.service.request.ImportFileRequest;
import org.osc.core.broker.service.response.BaseResponse;

public interface ImportSdnControllerPluginServiceApi
        extends ServiceDispatcherApi<ImportFileRequest, BaseResponse> {
}
