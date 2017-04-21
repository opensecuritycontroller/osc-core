package org.osc.core.broker.service.api;

import org.osc.core.broker.service.request.ListEncapsulationTypeByVersionTypeAndModelRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.sdk.controller.TagEncapsulationType;

public interface ListEncapsulationTypeByVersionTypeAndModelApi
        extends ServiceDispatcherApi<ListEncapsulationTypeByVersionTypeAndModelRequest, ListResponse<TagEncapsulationType>> {
}
