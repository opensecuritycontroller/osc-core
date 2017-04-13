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

import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.CheckNetworkSettingResponse;

public class CheckNetworkSettingsService extends ServiceDispatcher<Request, CheckNetworkSettingResponse> {

    @Override
    public CheckNetworkSettingResponse exec(Request request, EntityManager em) throws Exception {
        CheckNetworkSettingResponse response = new CheckNetworkSettingResponse();
        response.setHasDeployedInstances(DistributedApplianceInstanceEntityMgr.doesDAIExist(em));
        return response;
    }

}
