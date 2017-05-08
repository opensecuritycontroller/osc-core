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
package org.osc.core.broker.service.api.server;

public interface ServerApi {

    boolean getDevMode();

    void stopServer();

    void restart();

    String loadServerProp(String devModePropertyKey, String string);

    void saveServerProp(String propName, String value);

    void setDevMode(boolean on);

    boolean isUnderMaintenance();

    String getProductName();

    String getCurrentPid();

    String getVersionStr();
}
