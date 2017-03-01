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
package org.osc.core.rest.client.agent.model.input;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentUpdateVmidcServerRequest {

    private String vmidcServerIp;

    public String getVmidcServerIp() {
        return vmidcServerIp;
    }

    public void setVmidcServerIp(String vmidcServerIp) {
        this.vmidcServerIp = vmidcServerIp;
    }

    @Override
    public String toString() {
        return "AgentUpdateVmidcServerRequest [vmidcServerIp=" + vmidcServerIp + "]";
    }

}
