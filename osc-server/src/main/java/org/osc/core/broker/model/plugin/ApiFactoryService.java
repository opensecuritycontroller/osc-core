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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.sdk.controller.FlowInfo;
import org.osc.sdk.controller.FlowPortInfo;
import org.osc.sdk.controller.Status;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.manager.api.ManagerCallbackNotificationApi;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.api.ManagerDeviceMemberApi;
import org.osc.sdk.manager.api.ManagerDomainApi;
import org.osc.sdk.manager.api.ManagerPolicyApi;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;
import org.osc.sdk.manager.api.ManagerSecurityGroupInterfaceApi;
import org.osc.sdk.manager.api.ManagerWebSocketNotificationApi;
import org.osc.sdk.manager.element.ApplianceManagerConnectorElement;
import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface ApiFactoryService {
    String PLUGINS_DIRECTORY = "data/plugins";

    ApplianceManagerApi createApplianceManagerApi(String managerType) throws Exception;

    Boolean syncsPolicyMapping(String managerType) throws Exception;

    Boolean supportsMultiplePolicies(String managerType) throws Exception;

    Boolean supportsMultiplePolicies(VirtualSystem vs) throws Exception;

    Boolean syncsPolicyMapping(VirtualSystem vs) throws Exception;

    Boolean syncsSecurityGroup(String managerType) throws Exception;

    String getServiceName(String managerType) throws Exception;

    String getNotificationType(String managerType) throws Exception;

    Boolean providesDeviceStatus(String managerType) throws Exception;

    Boolean providesDeviceStatus(VirtualSystem virtualSystem) throws Exception;

    String getAuthenticationType(String managerType) throws Exception;

    boolean isBasicAuth(String managerType) throws Exception;

    boolean isKeyAuth(String managerType) throws Exception;

    String getExternalServiceName(String managerType) throws Exception;

    String getExternalServiceName(VirtualSystem virtualSystem) throws Exception;

    String getVendorName(String managerType) throws Exception;

    Boolean isPersistedUrlNotifications(ApplianceManagerConnector mc) throws Exception;

    boolean isWebSocketNotifications(ApplianceManagerConnector mc) throws Exception;

    ApplianceManagerConnectorElement getApplianceManagerConnectorElement(ApplianceManagerConnector mc)
            throws EncryptionException;

    ApplianceManagerConnectorElement getApplianceManagerConnectorElement(VirtualSystem vs) throws EncryptionException;

    ManagerWebSocketNotificationApi createManagerWebSocketNotificationApi(ApplianceManagerConnector mc)
            throws Exception;

    void checkConnection(ApplianceManagerConnector mc) throws Exception;

    ManagerDeviceMemberApi createManagerDeviceMemberApi(ApplianceManagerConnector mc, VirtualSystem vs)
            throws Exception;

    String generateServiceManagerName(VirtualSystem vs) throws Exception;

    ManagerDeviceApi createManagerDeviceApi(VirtualSystem vs) throws Exception;

    ManagerSecurityGroupInterfaceApi createManagerSecurityGroupInterfaceApi(VirtualSystem vs)
            throws Exception;

    /**
     * Creates a {@code SdnControllerApi} instance for the specified controller type.
     *
     * @param controllerType
     * @return
     * @throws Exception
     */
    SdnControllerApi createNetworkControllerApi(String controllerType) throws Exception;

    /**
     * gets specified property from {@code SdnControllerApi} instance for the specified controller type.
     *
     * @param controllerType
     * @param propertyName
     * @return
     * @throws Exception
     */
    Object getControllerPluginProperty(String controllerType, String propertyName) throws Exception;

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
     * @return
     */
    <T> PluginTracker<T> newPluginTracker(PluginTrackerCustomizer<T> customizer);

    Map<String, FlowPortInfo> queryPortInfo(VirtualizationConnector vc, String region,
            HashMap<String, FlowInfo> portsQuery) throws Exception;

    ManagerSecurityGroupApi createManagerSecurityGroupApi(VirtualSystem vs) throws Exception;

    ManagerPolicyApi createManagerPolicyApi(ApplianceManagerConnector mc) throws Exception;

    ManagerDomainApi createManagerDomainApi(ApplianceManagerConnector mc) throws Exception;

    Boolean syncsSecurityGroup(VirtualSystem vs) throws Exception;

    ManagerCallbackNotificationApi createManagerUrlNotificationApi(ApplianceManagerConnector mc) throws Exception;

    SdnRedirectionApi createNetworkRedirectionApi(VirtualSystem vs) throws Exception;

    SdnRedirectionApi createNetworkRedirectionApi(VirtualizationConnector vc) throws Exception;

    SdnRedirectionApi createNetworkRedirectionApi(DistributedApplianceInstance dai) throws Exception;

    SdnRedirectionApi createNetworkRedirectionApi(SecurityGroupMember sgm) throws Exception;

    Status getStatus(VirtualizationConnector vc, String region) throws Exception;

    Boolean supportsOffboxRedirection(VirtualSystem vs) throws Exception;

    Boolean supportsOffboxRedirection(SecurityGroup sg) throws Exception;

    Boolean supportsServiceFunctionChaining(SecurityGroup sg) throws Exception;

    Boolean supportsFailurePolicy(SecurityGroup sg) throws Exception;

    Boolean usesProviderCreds(String controllerType) throws Exception;

    Boolean providesTrafficPortInfo(String controllerType) throws Exception;

    Boolean supportsPortGroup(VirtualSystem vs) throws Exception;

    Boolean supportsPortGroup(SecurityGroup sg) throws Exception;

    Boolean supportsNeutronSFC(String controllerType) throws Exception;

    Boolean supportsNeutronSFC(VirtualSystem vs) throws Exception;

    Boolean supportsNeutronSFC(SecurityGroup sg) throws Exception;


}
