package org.osc.core.broker.model.plugin;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.view.maintenance.PluginUploader.PluginType;
import org.osc.core.server.installer.InstallableListener;
import org.osc.core.server.installer.InstallableManager;
import org.osc.core.server.installer.InstallableUnit;
import org.osc.core.server.installer.InstallableUnitEvent;
import org.osc.core.server.installer.State;
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

    private final static Logger LOGGER = Logger.getLogger(PluginTracker.class);
    /**
     * Marker property on Plugin services indicating the plugin name.
     */
    public static final String PROP_PLUGIN_NAME = "osc.plugin.name";

    /**
     * Marker property on Plugin services indicating the plugin vendor name.
     */
    public static final String PROP_PLUGIN_VENDOR_NAME = "osc.plugin.vendor_name";

    private static final Map<String, Class<?>> REQUIRED_PLUGIN_PROPERTIES =  ImmutableMap.<String, Class<?>>builder()
            .put(PROP_PLUGIN_NAME, String.class)
            .put(PROP_PLUGIN_VENDOR_NAME, String.class).build();

    private final BundleContext context;
    private final Class<T> pluginClass;
    private final PluginType pluginType;
    private final PluginTrackerCustomizer<T> customizer;
    private final InstallableManager installMgr;

    private final ServiceTracker<T, T> serviceTracker;
    private final InstallableListener installListener;

    private final Map<String, Plugin<T>> pluginMap = new HashMap<>();

    private ServiceRegistration<InstallableListener> installListenerReg;

    public PluginTracker(BundleContext context, Class<T> pluginClass, PluginType pluginType, InstallableManager installMgr, PluginTrackerCustomizer<T> customizer) {
        this.context = context;
        this.pluginClass = pluginClass;
        this.installMgr = installMgr;
        this.customizer = customizer;
        this.pluginType = pluginType;
        if (customizer == null) {
            throw new IllegalArgumentException("Null customizer on PluginTracker not permitted");
        }

        this.installListener = new InstallableListener() {
            @Override
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

        this.serviceTracker = new ServiceTracker<T,T>(context, pluginClass, null) {
            @Override
            public T addingService(ServiceReference<T> reference) {
                if (!containsRequiredProperties(reference, REQUIRED_PLUGIN_PROPERTIES)) {
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

        this.serviceTracker.open();
    }

    public void close() {
        this.installListenerReg.unregister();
        this.serviceTracker.close();
    }

    public static boolean containsRequiredProperties(ServiceReference<?> reference, Map<String, Class<?>> requiredProperties) {
        for (Map.Entry<String, Class<?>> entry : requiredProperties.entrySet()) {
            Object pluginNameObj = reference.getProperty(entry.getKey());
            if (pluginNameObj == null || !entry.getValue().isInstance(pluginNameObj)) {
                LOGGER.warn(String.format("Plugin service id=%d from bundle %s did not have %s property, or property was not a of type %s. Service ignored.", reference.getProperty(Constants.SERVICE_ID), reference.getBundle().getSymbolicName(), PROP_PLUGIN_NAME, entry.getValue()));
                return false; // returning null means we will never hear about this service again.
            }
        }

        return true;
    }

    private void addServiceForPlugin(String name, T service) {
        List<PluginEvent<T>> events = new LinkedList<>();
        synchronized (this.pluginMap) {
            Plugin<T> plugin = this.pluginMap.get(name);
            if (plugin == null) {
                // No current plugin, add a new plugin so we can remember this service
                plugin = new Plugin<>(this.pluginClass);
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

    private void updateUnit(InstallableUnit unit) {
        String name = unit.getName();
        String type = unit.getType();

        if (StringUtils.isBlank(type)) {
            throw new IllegalArgumentException("The provided installable unit must have a valid type.");
        }

        if (!type.equals(this.pluginType.toString())) {
            LOGGER.trace(String.format("Unit %s of type %s skipped by plugin tracker of type %s", name, type, this.pluginType));
            return;
        }

        List<PluginEvent<T>> events = new LinkedList<>();
        synchronized (this.pluginMap) {
            Plugin<T> plugin = this.pluginMap.get(name);
            if (plugin == null) {
                // No current plugin, add a new plugin if state==installed or state=error
                if (EnumSet.of(State.INSTALLED, State.ERROR).contains(unit.getState())) {
                    plugin = new Plugin<>(this.pluginClass);
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
