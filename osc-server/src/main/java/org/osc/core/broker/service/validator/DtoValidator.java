/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.validator;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.service.dto.BaseDto;

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

