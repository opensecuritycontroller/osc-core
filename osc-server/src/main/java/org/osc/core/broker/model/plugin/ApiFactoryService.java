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
package org.osc.core.broker.model.plugin;

import java.util.Set;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.view.maintenance.PluginUploader.PluginType;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface ApiFactoryService {
    /**
     * Creates an {@code ApplianceManagerApi} instance for the specified manager type.
     *
     * @param managerType
     * @return
     * @throws Exception
     */
    ApplianceManagerApi createApplianceManagerApi(ManagerType managerType) throws Exception;

    /**
     * Creates a {@code SdnControllerApi} instance for the specified controller type.
     *
     * @param controllerType
     * @return
     * @throws Exception
     */
    SdnControllerApi createNetworkControllerApi(ControllerType controllerType) throws Exception;

    /**
     * Creates a {@code VMwareSdnApi} instance for the specified {@code VirtualizationConnector} type.
     *
     * @param vc
     * @return
     * @throws VmidcException
     */
    VMwareSdnApi createVMwareSdnApi(VirtualizationConnector vc) throws VmidcException;

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

    /**
     * Creates a </<code>PluginTracker</code> to track installed units and the services that arise from
     * them. When an unit is installed but has not yet registered any services, a
     * Plugin is created in the INSTALL_WAIT state. When one or more services
     * appear, the Plugin transitions to the READY state. If all the services for a
     * Plugin go away, it transitions back to INSTALL_WAIT. Finally when the
     * installable unit is removed, the plugin will be removed.
     *
     * @param <T>
     *            The service type registered by the Plugin.
     *
     * @param customizer
     * @param pluginClass
     * @param pluginType
     * @return
     */
    <T> PluginTracker<T> newPluginTracker(PluginTrackerCustomizer<T> customizer, Class<T> pluginClass,
            PluginType pluginType);
}
