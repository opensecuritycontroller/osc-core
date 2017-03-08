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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.view.maintenance.PluginUploader.PluginType;
import org.osc.core.server.installer.InstallableManager;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(immediate = true)
public class ApiFactoryServiceImpl implements ApiFactoryService {

    private static final String OSC_PLUGIN_NAME = PluginTracker.PROP_PLUGIN_NAME;

    private Map<String, ApplianceManagerApi> managerApis = new ConcurrentHashMap<>();
    private Map<String, ComponentServiceObjects<ApplianceManagerApi>> managerRefs = new ConcurrentHashMap<>();
    private Map<String, VMwareSdnApi> vmwareSdnApis = new ConcurrentHashMap<>();
    private Map<String, ComponentServiceObjects<VMwareSdnApi>> vmwareSdnRefs = new ConcurrentHashMap<>();
    private Map<String, ComponentServiceObjects<SdnControllerApi>> sdnControllerRefs = new ConcurrentHashMap<>();

    private List<PluginTracker<?>> pluginTrackers = new LinkedList<>();

    @Reference
    private InstallableManager installableManager;

    private BundleContext context;

    @Activate
    void activate(BundleContext context) {
        this.context = context;
    }

    @Deactivate
    void deactivate() {
        synchronized (this.pluginTrackers) {
            this.pluginTrackers.forEach(tracker -> {
                try {
                    tracker.close();
                } catch (Exception e) {
                }
            });
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addApplianceManagerApi(ComponentServiceObjects<ApplianceManagerApi> serviceObjs) {
        Object name = serviceObjs.getServiceReference().getProperty(OSC_PLUGIN_NAME);
        if (name instanceof String) {
            this.managerRefs.put((String) name, serviceObjs);
        }
    }

    void removeApplianceManagerApi(ComponentServiceObjects<ApplianceManagerApi> serviceObjs) {
        Object name = serviceObjs.getServiceReference().getProperty(OSC_PLUGIN_NAME);
        if (name instanceof String) {
            this.managerRefs.remove(name);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addSdnControllerApi(ComponentServiceObjects<SdnControllerApi> serviceObjs) {
        Object name = serviceObjs.getServiceReference().getProperty(OSC_PLUGIN_NAME);
        if (name instanceof String) {
            this.sdnControllerRefs.put((String) name, serviceObjs);
        }
    }

    void removeSdnControllerApi(ComponentServiceObjects<SdnControllerApi> serviceObjs) {
        Object name = serviceObjs.getServiceReference().getProperty(OSC_PLUGIN_NAME);
        if (name instanceof String) {
            this.sdnControllerRefs.remove(name);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addVMwareSdnApi(ComponentServiceObjects<VMwareSdnApi> serviceObjs) {
        Object name = serviceObjs.getServiceReference().getProperty(OSC_PLUGIN_NAME);
        if (name instanceof String) {
            this.vmwareSdnRefs.put((String) name, serviceObjs);
        }
    }

    void removeVMwareSdnApi(ComponentServiceObjects<VMwareSdnApi> serviceObjs) {
        Object name = serviceObjs.getServiceReference().getProperty(OSC_PLUGIN_NAME);
        if (name instanceof String) {
            this.vmwareSdnRefs.remove(name);
        }
    }

    @Override
    public ApplianceManagerApi createApplianceManagerApi(ManagerType managerType) throws Exception {
        final String name = managerType.getValue();
        ApplianceManagerApi api = this.managerApis.get(name);

        if (api == null) {
            ComponentServiceObjects<ApplianceManagerApi> serviceObjs = this.managerRefs.get(name);
            if (serviceObjs == null) {
                throw new VmidcException(String.format("Manager plugin not found for controller type: %s", name));
            }
            api = serviceObjs.getService();
            this.managerApis.put(name, api);
        }

        return api;
    }

    @Override
    public VMwareSdnApi createVMwareSdnApi(VirtualizationConnector vc) throws VmidcException {
        final String name = vc.getControllerType();
        VMwareSdnApi api = this.vmwareSdnApis.get(name);

        if (api == null) {
            ComponentServiceObjects<VMwareSdnApi> serviceObjs = this.vmwareSdnRefs.get(name);
            if (serviceObjs == null) {
                throw new VmidcException(String.format("NSX plugin not found for controller type: %s", name));
            }
            api = serviceObjs.getService();
            this.vmwareSdnApis.put(name, api);
        }

        return api;
    }

    @Override
    public SdnControllerApi createNetworkControllerApi(ControllerType controllerType) throws Exception {
        final String name = controllerType.getValue();
        ComponentServiceObjects<SdnControllerApi> serviceObjs = this.sdnControllerRefs.get(name);
        if (serviceObjs == null) {
            throw new VmidcException(String.format("Sdn plugin not found for controller type: %s", name));
        }
        return autoCloseProxy(serviceObjs, SdnControllerApi.class);
    }

    private <T extends AutoCloseable> T autoCloseProxy(ComponentServiceObjects<T> serviceObjs, Class<T> type) {
        T service = serviceObjs.getService();

        @SuppressWarnings("unchecked")
        T proxy = (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { type },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("close") && method.getParameterTypes().length == 0) {
                            serviceObjs.ungetService(service);
                        }
                        return method.invoke(service, args);
                    }
                });
        return proxy;
    }

    @Override
    public Set<String> getManagerTypes() {
        return new TreeSet<String>(this.managerRefs.keySet());
    }

    @Override
    public Set<String> getControllerTypes() {
        Set<String> controllerTypes = new TreeSet<>();
        controllerTypes.addAll(this.sdnControllerRefs.keySet());
        controllerTypes.addAll(this.vmwareSdnRefs.keySet());
        return controllerTypes;
    }

    @Override
    public <T> PluginTracker<T> newPluginTracker(PluginTrackerCustomizer<T> customizer, Class<T> pluginClass,
            PluginType pluginType) {
        if (customizer == null) {
            throw new IllegalArgumentException("Plugin tracker customizer may not be null");
        }

        PluginTracker<T> tracker = new PluginTracker<>(this.context, pluginClass, pluginType, this.installableManager,
                customizer);
        synchronized (this.pluginTrackers) {
            this.pluginTrackers.add(tracker);
        }
        tracker.open();
        return tracker;
    }

}