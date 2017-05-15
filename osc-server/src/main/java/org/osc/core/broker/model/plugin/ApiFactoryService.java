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

import java.util.Map;
import java.util.Set;

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.view.maintenance.PluginUploader.PluginType;
import org.osc.core.util.encryption.EncryptionException;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.manager.api.ManagerDeviceMemberApi;
import org.osc.sdk.manager.api.ManagerWebSocketNotificationApi;
import org.osc.sdk.manager.element.ApplianceManagerConnectorElement;
import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface ApiFactoryService {
    /**
     * Creates an {@code ApplianceManagerApi} instance for the specified manager type.
     *
     * @param managerName
     * @return
     * @throws Exception
     */
    ApplianceManagerApi createApplianceManagerApi(ManagerType managerType) throws Exception;

    Boolean syncsPolicyMapping(ManagerType managerType) throws Exception;

    Boolean syncsSecurityGroup(ManagerType managerType) throws Exception;

    String getServiceName(ManagerType managerType) throws Exception;

    String getNotificationType(ManagerType managerType) throws Exception;

    Boolean providesDeviceStatus(ManagerType managerType) throws Exception;

    String getAuthenticationType(ManagerType managerType) throws Exception;

    boolean isBasicAuth(ManagerType managerType) throws Exception;

    boolean isKeyAuth(ManagerType managerType) throws Exception;

    String getExternalServiceName(ManagerType managerType) throws Exception;

    String getVendorName(ManagerType managerType) throws Exception;

    Boolean isPersistedUrlNotifications(ApplianceManagerConnector mc) throws Exception;

    boolean isWebSocketNotifications(ApplianceManagerConnector mc) throws Exception;

    ApplianceManagerConnectorElement getApplianceManagerConnectorElement(ApplianceManagerConnector mc)
            throws EncryptionException;

    ManagerWebSocketNotificationApi createManagerWebSocketNotificationApi(ApplianceManagerConnector mc)
            throws Exception;

    void checkConnection(ApplianceManagerConnector mc) throws Exception;

    ManagerDeviceMemberApi createManagerDeviceMemberApi(ApplianceManagerConnector mc, VirtualSystem vs)
            throws Exception;

    String generateServiceManagerName(VirtualSystem vs) throws Exception;

    /**
     * Creates a {@code SdnControllerApi} instance for the specified controller type.
     *
     * @param controllerType
     * @return
     * @throws Exception
     */
    SdnControllerApi createNetworkControllerApi(ControllerType controllerType) throws Exception;

    /**
     * gets specified property from {@code SdnControllerApi} instance for the specified manager type.
     *
     * @param managerType
     * @param propertyName
     * @return
     * @throws Exception
     */
    Object getPluginProperty(ControllerType controllerType, String propertyName) throws Exception;

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
     * @param requiredProperties
     * @return
     */
    <T> PluginTracker<T> newPluginTracker(PluginTrackerCustomizer<T> customizer, Class<T> pluginClass,
            PluginType pluginType, Map<String, Class<?>> requiredProperties);

}
