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

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.service.api.SetNATSettingsServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.dto.NATSettingsDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.validator.NATSettingsDtoValidator;
import org.osc.core.broker.util.ServerUtil;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.util.log.LogProvider;
import org.osc.core.server.Server;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

@Component
public class SetNATSettingsService extends ServiceDispatcher<DryRunRequest<NATSettingsDto>, BaseJobResponse>
        implements SetNATSettingsServiceApi {

    private static final Logger log = LogProvider.getLogger(SetNATSettingsService.class);

    @Reference
    private ServerApi server;

    @Reference
    private SetNetworkSettingsService setNetworkSettingsService;

    void validate(DryRunRequest<NATSettingsDto> req) throws Exception {
        NATSettingsDtoValidator.checkForNullFields(req.getDto());
        // check for valid IP address format
        ValidateUtil.checkForValidIpAddressFormat(req.getDto().getPublicIPAddress());
    }

    @Override
    public BaseJobResponse exec(DryRunRequest<NATSettingsDto> request, EntityManager em) throws Exception {
        String oldServerIp = ServerUtil.getServerIP();
        String newServerIp = request.getDto().getPublicIPAddress();

        this.server.saveServerProp(Server.ISC_PUBLIC_IP, newServerIp);
        // Update ServerUtil attribute as well
        ServerUtil.setServerIP(newServerIp);
        log.info("Successfully updated Server Public IP Address..");
        BaseJobResponse response = new BaseJobResponse();

        if (!StringUtils.equals(oldServerIp, newServerIp)) {
            response.setJobId(this.setNetworkSettingsService.startIpPropagateJob());
        }

        return response;
    }
}
