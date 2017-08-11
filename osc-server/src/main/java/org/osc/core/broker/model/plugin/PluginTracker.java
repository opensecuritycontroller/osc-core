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

import static org.osc.sdk.controller.Constants.*;
import static org.osc.sdk.manager.Constants.*;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osc.core.server.installer.InstallableListener;
import org.osc.core.server.installer.InstallableManager;
import org.osc.core.server.installer.InstallableUnit;
import org.osc.core.server.installer.InstallableUnitEvent;
import org.osc.core.server.installer.State;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.google.common.collect.ImmutableMap;

/**
 * This class keeps track of installed units and the services that arise from
 * them. When an unit is installed but has not yet registered any services, a
 * Plugin is created in the INSTALL_WAIT state. When one or more services
 * appear, the Plugin transitions to the READY state. If all the services for a
 * Plugin go away, it transitions back to INSTALL_WAIT. Finally when the
 * installable unit is removed, the plugin will be removed.
 *
 * Note that it's possible for services to appear before we know about the
 * installable unit. Therefore we have to keep track of these services and match
 * them up with the unit when it appears.
 *
 * @param <T>
 *            The service type registered by the Plugin.
 */
public class PluginTracker<T> {

    private final Logger LOGGER = Logger.getLogger(PluginTracker.class);

    /**
     * Marker property on Plugin services indicating the plugin name.
     */
    public static final String PROP_PLUGIN_NAME = "osc.plugin.name";


    private static final Map<String, Class<?>> REQUIRED_MANAGER_PLUGIN_PROPERTIES = ImmutableMap
            .<String, Class<?>>builder().put(VENDOR_NAME, String.class).put(SERVICE_NAME, String.class)
            .put(EXTERNAL_SERVICE_NAME, String.class).put(AUTHENTICATION_TYPE, String.class)
            .put(NOTIFICATION_TYPE, String.class).put(SYNC_SECURITY_GROUP, Boolean.class)
            .put(PROVIDE_DEVICE_STATUS, Boolean.class).put(SYNC_POLICY_MAPPING, Boolean.class).put(PROP_PLUGIN_NAME, String.class).build();

    private static final Map<String, Class<?>> REQUIRED_SDN_CONTROLLER_PLUGIN_PROPERTIES = ImmutableMap
            .<String, Class<?>>builder().put(SUPPORT_OFFBOX_REDIRECTION, Boolean.class).put(SUPPORT_SFC, Boolean.class)
            .put(SUPPORT_FAILURE_POLICY, Boolean.class).put(USE_PROVIDER_CREDS, Boolean.class)
            .put(QUERY_PORT_INFO, Boolean.class).put(SUPPORT_PORT_GROUP, Boolean.class).put(PROP_PLUGIN_NAME, String.class).build();

    private final BundleContext context;
    
    @SuppressWarnings("unchecked")
    private final Class<T> pluginClassManager = (Class<T>) ApplianceManagerApi.class;
    
    @SuppressWarnings("unchecked")
    private final Class<T> pluginClassSdn = (Class<T>) SdnControllerApi.class;

    private final PluginTrackerCustomizer<T> customizer;
    private final InstallableManager installMgr;

    private final ServiceTracker<T, T> serviceTrackerManager;
    private final ServiceTracker<T, T> serviceTrackerSdn;
    private final InstallableListener installListener;
    private final Map<String, Plugin<T>> pluginMap = new HashMap<>();

    private ServiceRegistration<InstallableListener> installListenerReg;

    public PluginTracker(BundleContext context, InstallableManager installMgr, PluginTrackerCustomizer<T> customizer) {
        this.context = context;
        this.installMgr = installMgr;
        this.customizer = customizer;

        if (customizer == null) {
            throw new IllegalArgumentException("Null customizer on PluginTracker not permitted");
        }

        this.installListener = new InstallableListener() {
            @Override
            @SuppressWarnings("fallthrough")
            public void installableUnitsChanged(Collection<InstallableUnitEvent> events) {
                for (InstallableUnitEvent event : events) {
                    switch (event.getNewState()) {
                    case INSTALLED:
                    case REMOVED:
                    case ERROR:
                        updateUnit(event.getUnit());
                    default:
                        // ignore
                    }
                }
            }

        };

        this.serviceTrackerManager = getServiceTracker(this.pluginClassManager, REQUIRED_MANAGER_PLUGIN_PROPERTIES);
        this.serviceTrackerSdn = getServiceTracker(this.pluginClassSdn, REQUIRED_SDN_CONTROLLER_PLUGIN_PROPERTIES);

    }

    private ServiceTracker<T,T> getServiceTracker(Class<T> pluginClass, Map<String, Class<?>> requiredPluginProperties) {
    	return new ServiceTracker<T,T>(this.context, pluginClass, null) {
            @Override
            public T addingService(ServiceReference<T> reference) {
                if (!containsRequiredProperties(reference, requiredPluginProperties)) {
                    return null;
                }

                Object pluginNameObj = reference.getProperty(PROP_PLUGIN_NAME);
                String pluginName = (String) pluginNameObj;

                T service = this.context.getService(reference);
                addServiceForPlugin(pluginName, service);
                return service;
            }
            @Override
            public void removedService(ServiceReference<T> reference, T service) {
                String pluginName = (String) reference.getProperty(PROP_PLUGIN_NAME); // Safe to assume non-null String: would otherwise have been rejected by addingService
                removeServiceForPlugin(pluginName, service);
                this.context.ungetService(reference);
            }
        };

    }

    @SuppressWarnings("fallthrough")
    public void open() {
        // Register listener first, before getting current installable units. We are
        // required to handle duplicates (better than missing some).
        this.installListenerReg = this.context.registerService(InstallableListener.class, this.installListener, null);
        for (InstallableUnit unit : this.installMgr.getInstallableUnits()) {
            switch (unit.getState()) {
            case INSTALLED:
            case REMOVED:
            case ERROR:
                updateUnit(unit);
            default:
                // ignore
            }
        }

        this.serviceTrackerManager.open();
        this.serviceTrackerSdn.open();
    }

    public void close() {
        this.installListenerReg.unregister();
        this.serviceTrackerManager.close();
        this.serviceTrackerSdn.close();
    }

    private void addServiceForPlugin(String name, T service) {
        List<PluginEvent<T>> events = new LinkedList<>();
        synchronized (this.pluginMap) {
            Plugin<T> plugin = this.pluginMap.get(name);
            if (plugin == null) {
                // No current plugin, add a new plugin so we can remember this service
                plugin = new Plugin<>();
                plugin.addService(service);

                this.pluginMap.put(name, plugin);
                // don't send an ADDING event because there is no InstallUnit on the plugin yet
            } else {
                // There is a current plugin, so add our service to the list
                if (plugin.addService(service)) {
                    events.add(new PluginEvent<>(PluginEvent.Type.MODIFIED, plugin));
                }
            }
        }
        for (PluginEvent<T> event : events) {
            this.customizer.pluginEvent(event);
        }
    }

    private void removeServiceForPlugin(String name, T service) {
        List<PluginEvent<T>> events = new LinkedList<>();

        synchronized (this.pluginMap) {
            Plugin<T> plugin = this.pluginMap.get(name);
            if (plugin == null) {
                // Nothing to do... removed service from already-forgotten plugin
            } else {
                if (plugin.removeService(service)) {
                    events.add(new PluginEvent<>(PluginEvent.Type.MODIFIED, plugin));
                }
            }
        }

        for (PluginEvent<T> event : events) {
            this.customizer.pluginEvent(event);
        }
    }

    private boolean containsRequiredProperties(ServiceReference<?> reference, Map<String, Class<?>> reqProperties) {
        for (Map.Entry<String, Class<?>> entry : reqProperties.entrySet()) {
            Object pluginNameObj = reference.getProperty(entry.getKey());
            if (pluginNameObj == null || !entry.getValue().isInstance(pluginNameObj)) {
                this.LOGGER.warn(String.format("Plugin service id=%d from bundle %s did not have %s property, or property was not a of type %s. Service ignored.", reference.getProperty(Constants.SERVICE_ID), reference.getBundle().getSymbolicName(), PROP_PLUGIN_NAME, entry.getValue()));
                return false; // returning null means we will never hear about this service again.
            }
        }

        return true;
    }

    private void updateUnit(InstallableUnit unit) {
        String name = unit.getName();

        List<PluginEvent<T>> events = new LinkedList<>();
        synchronized (this.pluginMap) {
            Plugin<T> plugin = this.pluginMap.get(name);
            if (plugin == null) {
                // No current plugin, add a new plugin if state==installed or state=error
                if (EnumSet.of(State.INSTALLED, State.ERROR).contains(unit.getState())) {
                    plugin = new Plugin<>();
                    plugin.setUnit(unit);
                    if (unit.getState() == State.ERROR) {
                        plugin.setError(unit.getErrorMessage());
                    }
                    this.pluginMap.put(name, plugin);

                    events.add(new PluginEvent<>(PluginEvent.Type.ADDING, plugin));
                }
            } else {
                // We have an existing plugin entry for this name.
                switch (unit.getState()) {
                case INSTALLED:
                    // This usually means we saw the services appear before the plugin itself appeared.
                    if (plugin.setUnit(unit)) {
                        events.add(new PluginEvent<>(PluginEvent.Type.ADDING, plugin));
                    }
                    break;
                case REMOVED:
                    // Existing install unit is being removed, fire event for uninstalled plugin
                    this.pluginMap.remove(name);
                    events.add(new PluginEvent<>(PluginEvent.Type.REMOVED, plugin));
                    break;
                case ERROR:
                    plugin.setUnit(unit);
                    if (plugin.setError(unit.getErrorMessage())) {
                        events.add(new PluginEvent<>(PluginEvent.Type.MODIFIED, plugin));
                    }
                    break;
                case INSTALLING:
                case RESOLVED:
                    // ignore
                default:
                    // ignore
                }
            }
        }

        for (PluginEvent<T> event : events) {
            this.customizer.pluginEvent(event);
        }
    }

}
