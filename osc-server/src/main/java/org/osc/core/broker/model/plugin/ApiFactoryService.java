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
    ApplianceManagerApi createApplianceManagerApi(ManagerType managerType) throws Exception;

    SdnControllerApi createNetworkControllerApi(ControllerType controllerType) throws Exception;

    VMwareSdnApi createVMwareSdnApi(VirtualizationConnector vc) throws VmidcException;

    Set<String> getManagerTypes();
    Set<String> getControllerTypes();

    <T> PluginTracker<T> newPluginTracker(PluginTrackerCustomizer<T> customizer, Class<T> pluginClass,
            PluginType pluginType);
}
