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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.PluginTracker;
import org.osc.core.broker.model.plugin.PluginTrackerCustomizer;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.view.maintenance.PluginUploader.PluginType;
import org.osc.core.server.installer.InstallableManager;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.encryption.EncryptionException;
import org.osc.sdk.manager.ManagerAuthenticationType;
import org.osc.sdk.manager.ManagerNotificationSubscriptionType;
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
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class ManagerApiFactory {

    public static final String MANAGER_PLUGINS_DIRECTORY = "mgr_plugins";

    private final static Logger log = Logger.getLogger(ManagerApiFactory.class);

    private static HashMap<String, ApplianceManagerApi> plugins = new HashMap<String, ApplianceManagerApi>();

    private static BundleContext bundleContext;
    private static ServiceTracker<InstallableManager, InstallableManager> installManagerTracker;
    private static List<PluginTracker<ApplianceManagerApi>> pluginTrackers = new LinkedList<>();
    private static ServiceTracker<ApplianceManagerApi, String> pluginServiceTracker;

    public static PluginTracker<ApplianceManagerApi> newPluginTracker(PluginTrackerCustomizer<ApplianceManagerApi> customizer, PluginType pluginType) throws ServiceUnavailableException {
        if (customizer == null) {
            throw new IllegalArgumentException("Plugin tracker customizer may not be null");
        }

        // TODO This is a horrible way to get hold of a service instance; if only we could use DS here.
        InstallableManager installMgr = null;
        try {
            installMgr = installManagerTracker.waitForService(2000);
        } catch (InterruptedException e) {
            // allow interrupted state to be cleared, installMgr remains null
        }
        if (installMgr == null) {
            throw new ServiceUnavailableException(InstallableManager.class.getName());
        }

        PluginTracker<ApplianceManagerApi> tracker;
        synchronized (pluginTrackers) {
            tracker = new PluginTracker<>(bundleContext, ApplianceManagerApi.class, pluginType, installMgr, customizer);
            pluginTrackers.add(tracker);
        }
        tracker.open();

        return tracker;
    }

    public static void init() throws Exception {
        bundleContext = FrameworkUtil.getBundle(ManagerApiFactory.class).getBundleContext();

        installManagerTracker = new ServiceTracker<>(bundleContext, InstallableManager.class, null);
        installManagerTracker.open();

        pluginServiceTracker = new ServiceTracker<ApplianceManagerApi, String>(bundleContext, ApplianceManagerApi.class, null) {
            @Override
            public String addingService(ServiceReference<ApplianceManagerApi> reference) {
                Object nameObj = reference.getProperty("osc.plugin.name");
                if (!(nameObj instanceof String)) {
                    return null;
                }

                String name = (String) nameObj;
                ApplianceManagerApi service = this.context.getService(reference);

                ApplianceManagerApi existing = plugins.putIfAbsent(name, service);
                if (existing != null) {
                    log.warn(String.format("Multiple plugin services of type %s available with name=%s", ApplianceManagerApi.class.getName(), name));
                    this.context.ungetService(reference);
                    return null;
                }
                ManagerType.addType(name);
                return name;
            }
            @Override
            public void removedService(ServiceReference<ApplianceManagerApi> reference, String name) {
                if (plugins.remove(name) != null) {
                    ManagerType.removeType(name);
                }
                this.context.ungetService(reference);
            }
        };
        pluginServiceTracker.open();
    }

    public static void shutdown() {
        pluginServiceTracker.close();
        synchronized (pluginTrackers) {
            while (!pluginTrackers.isEmpty()) {
                PluginTracker<ApplianceManagerApi> tracker = pluginTrackers.remove(0);
                try {
                    tracker.close();
                } catch (Exception e) {
                }
            }
        }
        installManagerTracker.close();
    }

    public static Set<String> getManagerTypes() {
        return plugins.keySet();
    }

    public static ApplianceManagerApi createApplianceManagerApi(ManagerType managerType) throws Exception {
        ApplianceManagerApi plugin = plugins.get(managerType.toString());
        if (plugin != null) {
            return plugin;
        } else {
            throw new VmidcException("Unsupported Manager type '" + managerType + "'");
        }
    }

    public static ApplianceManagerApi createApplianceManagerApi(DistributedApplianceInstance dai) throws Exception {
        return createApplianceManagerApi(dai.getVirtualSystem());
    }

    public static ApplianceManagerApi createApplianceManagerApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(getDecryptedApplianceManagerConnector(vs.getDistributedAppliance().getApplianceManagerConnector()).getManagerType());
    }

    public static ManagerDeviceApi createManagerDeviceApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createManagerDeviceApi(getApplianceManagerConnectorElement(vs), vs);
    }

    public static ManagerSecurityGroupInterfaceApi createManagerSecurityGroupInterfaceApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createManagerSecurityGroupInterfaceApi(getApplianceManagerConnectorElement(vs), vs);
    }

    public static ManagerSecurityGroupApi createManagerSecurityGroupApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createManagerSecurityGroupApi(getApplianceManagerConnectorElement(vs), vs);
    }

    public static ManagerPolicyApi createManagerPolicyApi(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerPolicyApi(getApplianceManagerConnectorElement(mc));
    }

    public static ManagerDomainApi createManagerDomainApi(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerDomainApi(getApplianceManagerConnectorElement(mc));
    }

    public static ManagerDeviceMemberApi createManagerDeviceMemberApi(ApplianceManagerConnector mc, VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerDeviceMemberApi(getApplianceManagerConnectorElement(mc), vs);
    }

    public static ManagerCallbackNotificationApi createManagerUrlNotificationApi(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerCallbackNotificationApi(getApplianceManagerConnectorElement(mc));
    }

    public static IscJobNotificationApi createIscJobNotificationApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createIscJobNotificationApi(getApplianceManagerConnectorElement(vs), vs);
    }

    public static boolean isPersistedUrlNotifications(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(getDecryptedApplianceManagerConnector(mc).getManagerType()).getNotificationType()
                .equals(ManagerNotificationSubscriptionType.CALLBACK_URL);
    }

    public static boolean isWebSocketNotifications(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(getDecryptedApplianceManagerConnector(mc).getManagerType()).getNotificationType()
                .equals(ManagerNotificationSubscriptionType.TRANSIENT_WEB_SOCKET);
    }

    public static boolean isBasicAuth(ManagerType mt) throws Exception {
        return createApplianceManagerApi(mt).getAuthenticationType().equals(ManagerAuthenticationType.BASIC_AUTH);
    }

    public static boolean isKeyAuth(ManagerType mt) throws Exception {
        return createApplianceManagerApi(mt).getAuthenticationType().equals(ManagerAuthenticationType.KEY_AUTH);
    }

    public static void checkConnection(ApplianceManagerConnector mc) throws Exception {
        createApplianceManagerApi(mc.getManagerType()).checkConnection(getApplianceManagerConnectorElement(mc));
    }

    private static ApplianceManagerConnector getDecryptedApplianceManagerConnector(ApplianceManagerConnector mc) throws EncryptionException {
        ApplianceManagerConnector shallowClone = new ApplianceManagerConnector(mc);
        if (!StringUtils.isEmpty(shallowClone.getPassword())) {
            shallowClone.setPassword(EncryptionUtil.decryptAESCTR(shallowClone.getPassword()));
        }
        return shallowClone;
    }

    public static ApplianceManagerConnectorElement getApplianceManagerConnectorElement(ApplianceManagerConnector mc) throws EncryptionException {
        ApplianceManagerConnector decryptedMc = getDecryptedApplianceManagerConnector(mc);

        // TODO emanoel: This will likely have some performance impact. We need to figure out an approach to keep these values cached on OSC
        // with some TTL and/or some refreshing mechanism not just for this scenario but potentially also others.
        decryptedMc.setClientIpAddress(ServerUtil.getServerIP());
        return decryptedMc;
    }

    static ManagerWebSocketNotificationApi createManagerWebSocketNotificationApi(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerWebSocketNotificationApi(getApplianceManagerConnectorElement(mc));
    }

    private static ApplianceManagerConnectorElement getApplianceManagerConnectorElement(VirtualSystem vs) throws EncryptionException {
        return getApplianceManagerConnectorElement(vs.getDistributedAppliance().getApplianceManagerConnector());
    }
}
