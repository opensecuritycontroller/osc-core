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

import java.util.Set;

import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface PluginService {
    /**
     * Creates an {@code ApplianceManagerApi} instance for the specified manager type.
     *
     * @param managerName
     * @return
     * @throws Exception
     */
    ApplianceManagerApi createApplianceManagerApi(String managerType) throws Exception;

    Boolean usesProviderCreds(String controllerType) throws Exception;

    boolean isKeyAuth(String managerType) throws Exception;

    /**
     * Gets the set of currently registered manager types.
     *
     * @return
     */
    Set<String> getManagerTypes();

    /**
     * Gets the set of currently registered controller types.
     *
     * @return
     */
    Set<String> getControllerTypes();

}
