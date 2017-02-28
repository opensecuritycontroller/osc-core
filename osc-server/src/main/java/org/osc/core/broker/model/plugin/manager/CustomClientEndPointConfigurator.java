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
package org.osc.core.broker.model.plugin.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpointConfig.Configurator;

public class CustomClientEndPointConfigurator extends Configurator {

    private String cookie;

    public CustomClientEndPointConfigurator(String cookie) {
        super();
        this.cookie = cookie;
    }

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        super.beforeRequest(headers);
        List<String> parameterList = headers.get("Cookie");
        if (parameterList == null) {
            parameterList = new ArrayList<>();
        }
        if (this.cookie != null) {
            parameterList.add(this.cookie);
            headers.put("Cookie", parameterList);
        }
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

}
