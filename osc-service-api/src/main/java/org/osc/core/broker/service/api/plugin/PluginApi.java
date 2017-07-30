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
package org.osc.core.broker.service.api.plugin;

import java.io.File;

public interface PluginApi {

    enum State {
    	/**
    	 * Plugin installed but no service published yet.
    	 */
    	INSTALL_WAIT,

    	/**
    	 * Plugin installed and service published.
    	 */
    	READY,

    	/**
    	 * Plugin not installed or ready due to an error.
    	 */
    	ERROR;
    }

    State getState();

    String getError();

    int getServiceCount();

    String getSymbolicName();

    String getVersion();

    String getName();

    File getOrigin();

}
