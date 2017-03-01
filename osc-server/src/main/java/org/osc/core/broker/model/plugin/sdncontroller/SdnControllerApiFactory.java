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
package org.osc.core.broker.model.plugin.sdncontroller;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.PluginTracker;
import org.osc.core.broker.model.plugin.PluginTrackerCustomizer;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ServiceUnavailableException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.view.maintenance.PluginUploader.PluginType;
import org.osc.core.server.installer.InstallableManager;
import org.osc.core.util.EncryptionUtil;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SdnControllerApiFactory {

    public static final String SDN_CONTROLLER_PLUGINS_DIRECTORY = "sdn_ctrl_plugins";

    private static HashMap<String, ServiceObjects<SdnControllerApi>> sdnControllerPlugins = new HashMap<>();
    private static HashMap<String, ServiceObjects<VMwareSdnApi>> vmWareSdnPlugins = new HashMap<>();

    private static BundleContext bundleContext;
    private static ServiceTracker<InstallableManager, InstallableManager> installManagerTracker;
    private static List<PluginTracker<SdnControllerApi>> sdnControllerPluginTrackers = new LinkedList<>();
    private static List<PluginTracker<VMwareSdnApi>> vmWareSdnPluginTrackers = new LinkedList<>();
    private static ServiceTracker<SdnControllerApi, String> sdnControllerPluginServiceTracker;
    private static ServiceTracker<VMwareSdnApi, String> vmWareSdnPluginServiceTracker;

    private final static Logger LOG = Logger.getLogger(SdnControllerApiFactory.class);

    public static SdnControllerApi createNetworkControllerApi(VirtualSystem vs) throws Exception {
        return createNetworkControllerApi(vs.getVirtualizationConnector(), null);
    }

    public static SdnControllerApi createNetworkControllerApi(VirtualizationConnector vc) throws Exception {
        return createNetworkControllerApi(vc, null);
    }

    public static SdnControllerApi createNetworkControllerApi(DistributedApplianceInstance dai) throws Exception {
        return createNetworkControllerApi(dai.getVirtualSystem(), dai.getDeploymentSpec().getRegion());
    }

    public static SdnControllerApi createNetworkControllerApi(SecurityGroupMember sgm) throws Exception {
        return createNetworkControllerApi(sgm.getSecurityGroup().getVirtualizationConnector(), sgm.getMemberRegion());
    }

    public static SdnControllerApi createNetworkControllerApi(VirtualSystem vs, String region) throws Exception {
        return createNetworkControllerApi(vs.getVirtualizationConnector(), region);
    }

    private static SdnControllerApi createNetworkControllerApi(VirtualizationConnector vc, String region)
            throws Exception {
        VirtualizationConnector shallowClone = new VirtualizationConnector(vc);
        SdnControllerApi sca = createNetworkControllerApi(shallowClone.getControllerType());
        shallowClone.setProviderPassword(EncryptionUtil.decryptAESCTR(shallowClone.getProviderPassword()));
        if (!StringUtils.isEmpty(shallowClone.getControllerPassword())) {
            shallowClone.setControllerPassword(EncryptionUtil.decryptAESCTR(shallowClone.getControllerPassword()));
        }
        sca.setVirtualizationConnector(shallowClone);
        sca.setRegion(region);
        return sca;
    }

    public static VMwareSdnApi createVMwareSdnApi(VirtualizationConnector vc) throws VmidcException  {
        ServiceObjects<VMwareSdnApi> plugin = vmWareSdnPlugins.get(vc.getControllerType().toString());
        if (plugin != null && plugin.getService() instanceof VMwareSdnApi) {
            return plugin.getService();
        } else {
            throw new VmidcException(String.format("NSX plugin not found for controller type: %s", vc.getControllerType().toString()));
        }
    }

    public static SdnControllerApi createNetworkControllerApi(ControllerType controllerType) throws Exception {
        ServiceObjects<SdnControllerApi> plugin = sdnControllerPlugins.get(controllerType.toString());
        if (plugin != null) {
            return plugin.getService();
        } else {
            throw new VmidcException("Unsupported Network Controller type.");
        }
    }

    public static <T> PluginTracker<T> newPluginTracker(PluginTrackerCustomizer<T> customizer, Class<T> pluginClass, PluginType pluginType) throws ServiceUnavailableException, VmidcException {
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

        PluginTracker<T> tracker = null;
        if (pluginClass == SdnControllerApi.class) {
            synchronized (sdnControllerPluginTrackers) {
                tracker = new PluginTracker<>(bundleContext, pluginClass, pluginType, installMgr, customizer);
                @SuppressWarnings("unchecked")
                PluginTracker<SdnControllerApi> sdnControllerTracker = (PluginTracker<SdnControllerApi>) tracker;
                sdnControllerPluginTrackers.add(sdnControllerTracker);
            }
        } else if (pluginClass == VMwareSdnApi.class) {
            synchronized (vmWareSdnPluginTrackers) {
                tracker = new PluginTracker<>(bundleContext, pluginClass, pluginType, installMgr, customizer);
                @SuppressWarnings("unchecked")
                PluginTracker<VMwareSdnApi> vmWareSdnTracker = (PluginTracker<VMwareSdnApi>) tracker;
                vmWareSdnPluginTrackers.add(vmWareSdnTracker);
            }
        } else {
            throw new VmidcException(String.format("Unsupported plugin class type %s", pluginClass));
        }

        tracker.open();

        return tracker;
    }

    public static void init() throws Exception {
        bundleContext = FrameworkUtil.getBundle(ManagerApiFactory.class).getBundleContext();

        installManagerTracker = new ServiceTracker<>(bundleContext, InstallableManager.class, null);
        installManagerTracker.open();
        sdnControllerPluginServiceTracker = createServiceTracker(SdnControllerApi.class);
        sdnControllerPluginServiceTracker.open();
        vmWareSdnPluginServiceTracker = createServiceTracker(VMwareSdnApi.class);
        vmWareSdnPluginServiceTracker.open();
    }

    private static <T> ServiceTracker<T, String> createServiceTracker(Class<T> pluginClass) {
        ServiceTracker<T, String> pluginServiceTracker = new ServiceTracker<T, String>(bundleContext, pluginClass, null) {
            @Override
            public String addingService(ServiceReference<T> reference) {
                Object nameObj = reference.getProperty("osc.plugin.name");
                if (!(nameObj instanceof String)) {
                    return null;
                }

                String name = (String) nameObj;
                ServiceObjects<T> serviceObjects = this.context.getServiceObjects(reference);

                ServiceObjects<?> existing = null;

                if (pluginClass == SdnControllerApi.class) {
                    @SuppressWarnings("unchecked")
                    ServiceObjects<SdnControllerApi> sdnControllerServiceObjects = (ServiceObjects<SdnControllerApi>) serviceObjects;
                    existing =  sdnControllerPlugins.putIfAbsent(name, sdnControllerServiceObjects);
                } else if (pluginClass == VMwareSdnApi.class){
                    @SuppressWarnings("unchecked")
                    ServiceObjects<VMwareSdnApi> vmWareSdnServiceObjects = (ServiceObjects<VMwareSdnApi>) serviceObjects;
                    existing = vmWareSdnPlugins.putIfAbsent(name, vmWareSdnServiceObjects);
                } else {
                    LOG.error(String.format("Unsupported plugin class type %s", pluginClass));
                    return null;
                }

                if (existing != null) {
                    LOG.warn(String.format("Multiple plugin services of type %s available with name=%s", ApplianceManagerApi.class.getName(), name));
                    this.context.ungetService(reference);
                    return null;
                }

                ControllerType.addType(name);
                return name;
            }
            @Override
            public void removedService(ServiceReference<T> reference, String name) {
                ServiceObjects<?> serviceObjects = null;
                if (pluginClass == SdnControllerApi.class) {
                    serviceObjects = sdnControllerPlugins.remove(name);
                } else if (pluginClass == VMwareSdnApi.class) {
                    serviceObjects = vmWareSdnPlugins.remove(name);
                }
                if (serviceObjects != null) {
                    ControllerType.removeType(name);
                }
                this.context.ungetService(reference);
            }
        };

        return pluginServiceTracker;
    }

    public static void shutdown() {
        sdnControllerPluginServiceTracker.close();
        vmWareSdnPluginServiceTracker.close();
        removeTrackers(sdnControllerPluginTrackers);
        removeTrackers(vmWareSdnPluginTrackers);
        installManagerTracker.close();
    }

    private static <T> void removeTrackers(List<PluginTracker<T>> trackers) {
        synchronized (trackers) {
            while (!trackers.isEmpty()) {
                PluginTracker<?> tracker = trackers.remove(0);
                try {
                    tracker.close();
                } catch (Exception e) {
                    LOG.warn("Exception thrown when closing the tracker.", e);
                }
            }
        }
    }

    public static Set<String> getControllerTypes() {
        Set<String> controllerTypes = new HashSet<>();
        controllerTypes.addAll(sdnControllerPlugins.keySet());
        controllerTypes.addAll(vmWareSdnPlugins.keySet());
        return controllerTypes;
    }
}
