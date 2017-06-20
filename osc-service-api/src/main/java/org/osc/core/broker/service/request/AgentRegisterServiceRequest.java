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
package org.osc.core.broker.service.request;

public class AgentRegisterServiceRequest extends AgentRegisterRequest implements Request {

    public AgentRegisterServiceRequest() {

    }

    public AgentRegisterServiceRequest(AgentRegisterRequest request) {
        setAgentVersion(request.getAgentVersion());
        setApplianceIp(request.getApplianceIp());
        setApplianceGateway(request.getApplianceGateway());
        setApplianceSubnetMask(request.getApplianceSubnetMask());
        setName(request.getName());
        setVirtualSystemId(request.getVsId());

        setDiscovered(request.isDiscovered());
        setInspectionReady(request.isInspectionReady());

        setCpaPid(request.getCpaPid());
        setCurrentServerTime(request.getCurrentServerTime());
        setCpaUptime(request.getCpaUptime());

        setAgentDpaInfo(request.getAgentDpaInfo());
    }

}
