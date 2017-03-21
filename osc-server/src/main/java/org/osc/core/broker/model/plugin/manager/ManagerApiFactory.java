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

import static org.osc.sdk.manager.Constants.AUTHENTICATION_TYPE;
import static org.osc.sdk.manager.Constants.EXTERNAL_SERVICE_NAME;
import static org.osc.sdk.manager.Constants.NOTIFICATION_TYPE;
import static org.osc.sdk.manager.Constants.PROVIDE_DEVICE_STATUS;
import static org.osc.sdk.manager.Constants.SERVICE_NAME;
import static org.osc.sdk.manager.Constants.SYNC_POLICY_MAPPING;
import static org.osc.sdk.manager.Constants.SYNC_SECURITY_GROUP;
import static org.osc.sdk.manager.Constants.VENDOR_NAME;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.PluginTracker;
import org.osc.core.broker.model.plugin.PluginTrackerCustomizer;
import org.osc.core.broker.view.maintenance.PluginUploader.PluginType;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.manager.api.IscJobNotificationApi;
import org.osc.sdk.manager.api.ManagerCallbackNotificationApi;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.api.ManagerDeviceMemberApi;
import org.osc.sdk.manager.api.ManagerDomainApi;
import org.osc.sdk.manager.api.ManagerPolicyApi;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;
import org.osc.sdk.manager.api.ManagerSecurityGroupInterfaceApi;
import org.osc.sdk.manager.api.ManagerWebSocketNotificationApi;
import org.osc.sdk.manager.element.ApplianceManagerConnectorElement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import com.google.common.collect.ImmutableMap;

public class ManagerApiFactory {
    public static final String MANAGER_PLUGINS_DIRECTORY = "mgr_plugins";
    private static final Logger log = Logger.getLogger(ManagerApiFactory.class);

    private static ApiFactoryService apiFactoryService;
    private static BundleContext bundleContext;

    private static final Map<String, Class<?>> REQUIRED_MANAGER_PLUGIN_PROPERTIES = ImmutableMap
            .<String, Class<?>>builder().put(VENDOR_NAME, String.class).put(SERVICE_NAME, String.class)
            .put(EXTERNAL_SERVICE_NAME, String.class).put(AUTHENTICATION_TYPE, String.class)
            .put(NOTIFICATION_TYPE, String.class).put(SYNC_SECURITY_GROUP, Boolean.class)
            .put(PROVIDE_DEVICE_STATUS, Boolean.class).put(SYNC_POLICY_MAPPING, Boolean.class).build();

    public static PluginTracker<ApplianceManagerApi> newPluginTracker(
            PluginTrackerCustomizer<ApplianceManagerApi> customizer, PluginType pluginType)
            throws ServiceUnavailableException {
        return apiFactoryService.newPluginTracker(customizer, ApplianceManagerApi.class, pluginType,
                REQUIRED_MANAGER_PLUGIN_PROPERTIES);
    }

    public static void init() throws Exception {
        bundleContext = FrameworkUtil.getBundle(ManagerApiFactory.class).getBundleContext();

        ServiceTracker<ApiFactoryService, ApiFactoryService> apiFactoryTracker = new ServiceTracker<>(bundleContext,
                ApiFactoryService.class, null);
        apiFactoryTracker.open();

        // TODO This is a horrible way to get hold of a service instance; if only we could use DS here.
        try {
            apiFactoryService = apiFactoryTracker.waitForService(2000);
            apiFactoryTracker.close();
        } catch (InterruptedException e) {
            // allow interrupted state to be cleared, apiFactoryService remains null
            log.error("InterruptedException waiting for ApiFactoryService");
        }

        if (apiFactoryService == null) {
            throw new ServiceUnavailableException(ApiFactoryService.class.getName());
        }
    }

    public static Set<String> getManagerTypes() {
        return apiFactoryService.getManagerTypes();
    }

    public static ApplianceManagerApi createApplianceManagerApi(String managerName) throws Exception {
        return apiFactoryService.createApplianceManagerApi(ManagerType.fromText(managerName));
    }

    public static ApplianceManagerApi createApplianceManagerApi(ManagerType managerType) throws Exception {
        return createApplianceManagerApi(managerType.getValue());
    }

    public static ApplianceManagerApi createApplianceManagerApi(DistributedApplianceInstance dai) throws Exception {
        return createApplianceManagerApi(dai.getVirtualSystem());
    }

    public static ApplianceManagerApi createApplianceManagerApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(
                getDecryptedApplianceManagerConnector(vs.getDistributedAppliance().getApplianceManagerConnector())
                        .getManagerType());
    }

    public static ManagerDeviceApi createManagerDeviceApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createManagerDeviceApi(getApplianceManagerConnectorElement(vs), new VirtualSystemElementImpl(vs));
    }

    public static ManagerSecurityGroupInterfaceApi createManagerSecurityGroupInterfaceApi(VirtualSystem vs)
            throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createManagerSecurityGroupInterfaceApi(getApplianceManagerConnectorElement(vs),
                        new VirtualSystemElementImpl(vs));
    }

    public static ManagerSecurityGroupApi createManagerSecurityGroupApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createManagerSecurityGroupApi(getApplianceManagerConnectorElement(vs),
                        new VirtualSystemElementImpl(vs));
    }

    public static ManagerPolicyApi createManagerPolicyApi(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerPolicyApi(getApplianceManagerConnectorElement(mc));
    }

    public static ManagerDomainApi createManagerDomainApi(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerDomainApi(getApplianceManagerConnectorElement(mc));
    }

    public static Boolean syncsSecurityGroup(ManagerType managerType) throws Exception {
        return apiFactoryService.syncsSecurityGroup(managerType);
    }

    public static Boolean syncsSecurityGroup(VirtualSystem vs) throws Exception {
        return syncsSecurityGroup(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    public static Boolean providesDeviceStatus(VirtualSystem vs) throws Exception {
        return apiFactoryService.providesDeviceStatus(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    public static Boolean syncsPolicyMapping(ManagerType managerType) throws Exception {
        return apiFactoryService.syncsPolicyMapping(managerType);
    }

    public static Boolean syncsPolicyMapping(VirtualSystem vs) throws Exception {
        return syncsPolicyMapping(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    public static String getExternalServiceName(VirtualSystem vs) throws Exception {
        return apiFactoryService.getExternalServiceName(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    public static String getServiceName(ManagerType managerType) throws Exception {
        return apiFactoryService.getServiceName(managerType);
    }

    public static String getVendorName(VirtualSystem vs) throws Exception {
        return apiFactoryService.getVendorName(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    public static ManagerDeviceMemberApi createManagerDeviceMemberApi(ApplianceManagerConnector mc, VirtualSystem vs)
            throws Exception {
        return createApplianceManagerApi(mc.getManagerType()).createManagerDeviceMemberApi(
                getApplianceManagerConnectorElement(mc), new VirtualSystemElementImpl(vs));
    }

    public static ManagerCallbackNotificationApi createManagerUrlNotificationApi(ApplianceManagerConnector mc)
            throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerCallbackNotificationApi(getApplianceManagerConnectorElement(mc));
    }

    public static IscJobNotificationApi createIscJobNotificationApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createIscJobNotificationApi(getApplianceManagerConnectorElement(vs), new VirtualSystemElementImpl(vs));
    }

    public static boolean isPersistedUrlNotifications(ApplianceManagerConnector mc) throws Exception {
        return apiFactoryService.isPersistedUrlNotifications(mc);
    }

    public static boolean isWebSocketNotifications(ApplianceManagerConnector mc) throws Exception {
        return apiFactoryService.isWebSocketNotifications(mc);
    }

    public static boolean isBasicAuth(ManagerType mt) throws Exception {
        return apiFactoryService.isBasicAuth(mt);
    }

    public static boolean isKeyAuth(ManagerType mt) throws Exception {
        return apiFactoryService.isKeyAuth(mt);
    }

    public static void checkConnection(ApplianceManagerConnector mc) throws Exception {
        createApplianceManagerApi(mc.getManagerType()).checkConnection(getApplianceManagerConnectorElement(mc));
    }

    private static ApplianceManagerConnector getDecryptedApplianceManagerConnector(ApplianceManagerConnector mc)
            throws EncryptionException {
        ApplianceManagerConnector shallowClone = new ApplianceManagerConnector(mc);
        if (!StringUtils.isEmpty(shallowClone.getPassword())) {
            shallowClone.setPassword(EncryptionUtil.decryptAESCTR(shallowClone.getPassword()));
        }
        return shallowClone;
    }

    public static ApplianceManagerConnectorElement getApplianceManagerConnectorElement(ApplianceManagerConnector mc)
            throws EncryptionException {
        return apiFactoryService.getApplianceManagerConnectorElement(mc);
    }

    static ManagerWebSocketNotificationApi createManagerWebSocketNotificationApi(ApplianceManagerConnector mc)
            throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerWebSocketNotificationApi(getApplianceManagerConnectorElement(mc));
    }

    private static ApplianceManagerConnectorElement getApplianceManagerConnectorElement(VirtualSystem vs)
            throws EncryptionException {
        return getApplianceManagerConnectorElement(vs.getDistributedAppliance().getApplianceManagerConnector());
    }

}
