package org.osc.core.broker.service.dto;

import org.osc.core.broker.model.entities.BaseEntity;

/**
 * This interface contains the contract used to validate {@link BaseDto} objects.
 */
public interface DtoValidator<T extends BaseDto, E extends BaseEntity> {
    /**
     * Validates the provided dto object for creation.
     * @param dto
     *              The object to be validated.
     * @throws Exception
     *              When the validation fails.
     */
    void validateForCreate(T dto) throws Exception;


    /**
     * Validates the provided dto object for update.
     * @param dto
     *              The object to be validated.
     * @return
     *              The corresponding entity.
     * @throws Exception
     *              When the validation fails.
     */
    E validateForUpdate(T dto) throws Exception;
}

