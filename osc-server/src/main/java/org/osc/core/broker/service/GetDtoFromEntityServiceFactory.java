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
package org.osc.core.broker.service;

import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.BaseDto;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * DS services cannot be registered with or looked up by generic information, only by type.
 *
 * This factory provides a {@link #getService(Class)} method to get an appropriately typed instance.
 *
 * To avoid using a factory like this, and to use DS directly, we would need refactor GetDtoFromEntity not to be generic:
 * <UL><LI>
 * We could create multiple versions of the API representing specific types, and register each of those as a DS
 * services.
 * </LI><LI>
 * We could add multiple versions of each method to a non-generic interface.
 * </UL>
 */
@Component
public class GetDtoFromEntityServiceFactory implements GetDtoFromEntityServiceFactoryApi {

    @Reference
    private UserContextApi userContext;

    @Reference
    private EncryptionApi encrypter;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Override
    public <T extends BaseDto> GetDtoFromEntityServiceApi<T> getService(Class<T> type) {
        return new GetDtoFromEntityService<T>(this.userContext, this.encrypter, this.apiFactoryService);
    }

}
