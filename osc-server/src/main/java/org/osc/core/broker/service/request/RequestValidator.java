package org.osc.core.broker.service.request;

import org.osc.core.broker.model.entities.BaseEntity;

/**
 * This interface contains the contract used to validate {@link Request} objects.
 */
public interface RequestValidator<T extends Request, E extends BaseEntity> {
    /**
     * Validates the provided request.
     * @param dto
     *              The object to be validated.
     * @throws Exception
     *              When the validation fails.
     */
    void validate(T dto) throws Exception;


    /**
     * Validates the provided request.
     * @param dto
     *              The request to be validated.
     * @return
     *              The corresponding entity loaded as part of the validation.
     * @throws Exception
     *              When the validation fails.
     */
    E validateAndLoad(T dto) throws Exception;
}