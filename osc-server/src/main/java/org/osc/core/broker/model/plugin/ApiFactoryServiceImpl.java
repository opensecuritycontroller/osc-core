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

import static org.osc.sdk.manager.Constants.AUTHENTICATION_TYPE;
import static org.osc.sdk.manager.Constants.EXTERNAL_SERVICE_NAME;
import static org.osc.sdk.manager.Constants.NOTIFICATION_TYPE;
import static org.osc.sdk.manager.Constants.PROVIDE_DEVICE_STATUS;
import static org.osc.sdk.manager.Constants.SERVICE_NAME;
import static org.osc.sdk.manager.Constants.SYNC_POLICY_MAPPING;
import static org.osc.sdk.manager.Constants.SYNC_SECURITY_GROUP;
import static org.osc.sdk.manager.Constants.VENDOR_NAME;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ApplianceManagerConnectorElementImpl;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.view.maintenance.PluginUploader.PluginType;
import org.osc.core.server.installer.InstallableManager;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.encryption.EncryptionException;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.manager.ManagerAuthenticationType;
import org.osc.sdk.manager.ManagerNotificationSubscriptionType;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.manager.element.ApplianceManagerConnectorElement;
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
    private final Logger log = Logger.getLogger(ApiFactoryServiceImpl.class);

    private Map<String, ApplianceManagerApi> managerApis = new ConcurrentHashMap<>();
    private Map<String, ComponentServiceObjects<ApplianceManagerApi>> managerRefs = new ConcurrentHashMap<>();
    private Map<String, VMwareSdnApi> vmwareSdnApis = new ConcurrentHashMap<>();
    private Map<String, ComponentServiceObjects<VMwareSdnApi>> vmwareSdnRefs = new ConcurrentHashMap<>();
    private Map<String, ComponentServiceObjects<SdnControllerApi>> sdnControllerRefs = new ConcurrentHashMap<>();

    private List<PluginTracker<?>> pluginTrackers = new LinkedList<>();

    @Reference
    private InstallableManager installableManager;

    private BundleContext context;

    private static ApiFactoryServiceImpl instance = null;

    /**
     * don't use this, it's a necessary evil until we fix static callers
     */
    @Deprecated
    public static ApiFactoryService instance() {
        return instance;
    }

    @Activate
    void activate(BundleContext context) {
        this.context = context;
        ApiFactoryServiceImpl.instance = this;
    }

    @Deactivate
    void deactivate() {
        synchronized (this.pluginTrackers) {
            this.pluginTrackers.forEach(tracker -> {
                try {
                    tracker.close();
                } catch (Exception e) {
                    this.log.debug("Error closing tracker: " + e, e);
                }
            });
        }
    }

    private <T> void addApi(ComponentServiceObjects<T> serviceObjs, Map<String, ComponentServiceObjects<T>> refs) {
        Object name = serviceObjs.getServiceReference().getProperty(OSC_PLUGIN_NAME);
        if (name instanceof String) {
            refs.put((String) name, serviceObjs);
            this.log.info("add plugin: " + name);
        } else {
            this.log.warn(String.format("add plugin ignored as %s=%s", OSC_PLUGIN_NAME, name));
        }
    }

    private <T> void removeApi(ComponentServiceObjects<T> serviceObjs, Map<String, ComponentServiceObjects<T>> refs) {
        Object name = serviceObjs.getServiceReference().getProperty(OSC_PLUGIN_NAME);
        if (name instanceof String) {
            refs.remove(name, serviceObjs);
            this.log.info("remove plugin: " + name);
        } else {
            this.log.warn(String.format("remove plugin ignored as %s=%s", OSC_PLUGIN_NAME, name));
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addApplianceManagerApi(ComponentServiceObjects<ApplianceManagerApi> serviceObjs) {
        addApi(serviceObjs, this.managerRefs);
    }

    void removeApplianceManagerApi(ComponentServiceObjects<ApplianceManagerApi> serviceObjs) {
        removeApi(serviceObjs, this.managerRefs);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addSdnControllerApi(ComponentServiceObjects<SdnControllerApi> serviceObjs) {
        addApi(serviceObjs, this.sdnControllerRefs);
    }

    void removeSdnControllerApi(ComponentServiceObjects<SdnControllerApi> serviceObjs) {
        removeApi(serviceObjs, this.sdnControllerRefs);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addVMwareSdnApi(ComponentServiceObjects<VMwareSdnApi> serviceObjs) {
        addApi(serviceObjs, this.vmwareSdnRefs);
    }

    void removeVMwareSdnApi(ComponentServiceObjects<VMwareSdnApi> serviceObjs) {
        addApi(serviceObjs, this.vmwareSdnRefs);
    }

    // Manager Types ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ApplianceManagerApi createApplianceManagerApi(String name) throws Exception {
        ApplianceManagerApi api = this.managerApis.get(name);

        if (api == null) {
            // ApplianceManagerApi is not a prototype service,
            // but the single service instance is use-counted.
            // We save the service object in the managerApis map, to avoid incrementing the count on each call.
            ComponentServiceObjects<ApplianceManagerApi> serviceObjs = this.managerRefs.get(name);
            if (serviceObjs == null) {
                throw new VmidcException(String.format("Manager plugin not found for controller type: %s", name));
            }
            api = serviceObjs.getService();
            this.managerApis.put(name, api);
        }

        return api;
    }

    private Object getPluginProperty(ManagerType managerType, String propertyName) throws Exception {
        final String name = managerType.getValue();
        if (!this.managerRefs.containsKey(name)) {
            throw new VmidcException("Unsupported Manager type '" + name + "'");
        }
        return this.managerRefs.get(name).getServiceReference().getProperty(propertyName);
    }

    @Override
    public Boolean syncsPolicyMapping(ManagerType managerType) throws Exception {
        return (Boolean) getPluginProperty(managerType, SYNC_POLICY_MAPPING);
    }

    @Override
    public Boolean syncsSecurityGroup(ManagerType managerType) throws Exception {
        return (Boolean) getPluginProperty(managerType, SYNC_SECURITY_GROUP);
    }

    @Override
    public String getServiceName(ManagerType managerType) throws Exception {
        return (String) getPluginProperty(managerType, SERVICE_NAME);
    }

    @Override
    public String getNotificationType(ManagerType managerType) throws Exception {
        return (String) getPluginProperty(managerType, NOTIFICATION_TYPE);
    }

    @Override
    public Boolean providesDeviceStatus(ManagerType managerType) throws Exception {
        return (Boolean) getPluginProperty(managerType, PROVIDE_DEVICE_STATUS);
    }

    @Override
    public String getAuthenticationType(ManagerType managerType) throws Exception {
        return (String) getPluginProperty(managerType, AUTHENTICATION_TYPE);
    }

    @Override
    public boolean isBasicAuth(ManagerType mt) throws Exception {
        return getAuthenticationType(mt).equals(ManagerAuthenticationType.BASIC_AUTH.toString());
    }

    @Override
    public boolean isKeyAuth(ManagerType mt) throws Exception {
        return getAuthenticationType(mt).equals(ManagerAuthenticationType.KEY_AUTH.toString());
    }

    @Override
    public String getExternalServiceName(ManagerType managerType) throws Exception {
        return (String) getPluginProperty(managerType, EXTERNAL_SERVICE_NAME);
    }

    @Override
    public String getVendorName(ManagerType managerType) throws Exception {
        return (String) getPluginProperty(managerType, VENDOR_NAME);
    }

    @Override
    public boolean isPersistedUrlNotifications(ApplianceManagerConnector mc) throws Exception {
        return getNotificationType(ManagerType.fromText(getDecryptedApplianceManagerConnector(mc).getManagerType()))
                .equals(ManagerNotificationSubscriptionType.CALLBACK_URL.toString());
    }

    @Override
    public boolean isWebSocketNotifications(ApplianceManagerConnector mc) throws Exception {
        return getNotificationType(ManagerType.fromText(getDecryptedApplianceManagerConnector(mc).getManagerType()))
                .equals(ManagerNotificationSubscriptionType.TRANSIENT_WEB_SOCKET.toString());
    }

    private ApplianceManagerConnector getDecryptedApplianceManagerConnector(ApplianceManagerConnector mc)
            throws EncryptionException {
        ApplianceManagerConnector shallowClone = new ApplianceManagerConnector(mc);
        if (!StringUtils.isEmpty(shallowClone.getPassword())) {
            shallowClone.setPassword(EncryptionUtil.decryptAESCTR(shallowClone.getPassword()));
        }
        return shallowClone;
    }

    @Override
    public ApplianceManagerConnectorElement getApplianceManagerConnectorElement(ApplianceManagerConnector mc)
            throws EncryptionException {
        ApplianceManagerConnector decryptedMc = getDecryptedApplianceManagerConnector(mc);

        // TODO emanoel: This will likely have some performance impact. We need to figure out an approach to keep these values cached on OSC
        // with some TTL and/or some refreshing mechanism not just for this scenario but potentially also others.
        decryptedMc.setClientIpAddress(ServerUtil.getServerIP());
        return new ApplianceManagerConnectorElementImpl(decryptedMc);
    }

    // Controller Types ///////////////////////////////////////////////////////////////////////////////////////////

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
        /*
         * The SdnControllerApi is a prototype service: @Component(scope=ServiceScope.PROTOTYPE).
         * This means that serviceObjs.getService() will return a new service instance
         * on each call. We need to arrange for serviceObjs.ungetService() to be called.
         * This is done by the autoCloseProxy.
         */
        return autoCloseProxy(serviceObjs, SdnControllerApi.class);
    }

    @Override
    public Object getPluginProperty(ControllerType controllerType, String propertyName) throws Exception {
        final String name = controllerType.getValue();
        if (!this.sdnControllerRefs.containsKey(name)) {
            throw new VmidcException("Unsupported Controller type '" + name + "'");
        }
        return this.sdnControllerRefs.get(name).getServiceReference().getProperty(propertyName);
    }

    /**
     * Create a proxy for an <code>AutoCloseable</code> prototype service, that automatically calls
     * {@code ServiceObjects#ungetService(Object)} when {@code AutoCloseable#close()} is called.
     *
     * @param serviceObjs
     * @param type
     * @return
     */
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
            PluginType pluginType, Map<String, Class<?>> requiredProperties) {
        if (customizer == null) {
            throw new IllegalArgumentException("Plugin tracker customizer may not be null");
        }

        PluginTracker<T> tracker = new PluginTracker<>(this.context, pluginClass, pluginType, requiredProperties,
                this.installableManager, customizer);
        synchronized (this.pluginTrackers) {
            this.pluginTrackers.add(tracker);
        }
        tracker.open();
        return tracker;
    }

}
