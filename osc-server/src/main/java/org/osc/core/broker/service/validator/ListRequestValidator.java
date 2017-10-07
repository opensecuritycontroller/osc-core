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

import java.util.List;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.service.request.Request;

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
