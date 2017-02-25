/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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