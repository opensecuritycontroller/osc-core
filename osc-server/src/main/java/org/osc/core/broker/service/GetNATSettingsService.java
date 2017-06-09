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

import javax.persistence.EntityManager;

import org.osc.core.broker.service.api.GetNATSettingsServiceApi;
import org.osc.core.broker.service.dto.NATSettingsDto;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.util.ServerUtil;
import org.osgi.service.component.annotations.Component;

@Component
public class GetNATSettingsService extends ServiceDispatcher<Request, BaseDtoResponse<NATSettingsDto>>
        implements GetNATSettingsServiceApi {
    @Override
    public BaseDtoResponse<NATSettingsDto> exec(Request request, EntityManager em) throws Exception {
        BaseDtoResponse<NATSettingsDto> response = new BaseDtoResponse<NATSettingsDto>();
        NATSettingsDto dto = new NATSettingsDto(ServerUtil.getServerIP());
        response.setDto(dto);
        return response;
    }
}
