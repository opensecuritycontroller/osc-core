package org.osc.core.broker.service.request;

import org.osc.core.broker.model.entities.BaseEntity;

import java.util.List;

public interface ListRequestValidator<T extends Request, E extends BaseEntity> extends RequestValidator<T, E> {
    /**
     * Validates the provided request.
     * @param request
     *              The request to be validated.
     * @return
     *              The corresponding list of entities loaded as part of the validation.
     * @throws Exception
     *              When the validation fails.
     */
    List<E> validateAndLoadList(T request) throws Exception;
}
