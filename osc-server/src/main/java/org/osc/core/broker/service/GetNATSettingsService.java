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
package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.service.dto.NATSettingsDto;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.util.ServerUtil;

public class GetNATSettingsService extends ServiceDispatcher<Request, BaseDtoResponse<NATSettingsDto>> {
    @Override
    public BaseDtoResponse<NATSettingsDto> exec(Request request, Session session) throws Exception {
        BaseDtoResponse<NATSettingsDto> response = new BaseDtoResponse<NATSettingsDto>();
        NATSettingsDto dto = new NATSettingsDto(ServerUtil.getServerIP());
        response.setDto(dto);
        return response;
    }
}
