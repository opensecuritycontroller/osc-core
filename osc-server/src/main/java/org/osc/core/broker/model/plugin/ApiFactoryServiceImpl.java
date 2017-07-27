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

import static java.util.Collections.emptyList;
import static org.osc.sdk.controller.Constants.QUERY_PORT_INFO;
import static org.osc.sdk.controller.Constants.SUPPORT_FAILURE_POLICY;
import static org.osc.sdk.controller.Constants.SUPPORT_OFFBOX_REDIRECTION;
import static org.osc.sdk.controller.Constants.SUPPORT_PORT_GROUP;
import static org.osc.sdk.controller.Constants.SUPPORT_SFC;
import static org.osc.sdk.controller.Constants.USE_PROVIDER_CREDS;
import static org.osc.sdk.manager.Constants.AUTHENTICATION_TYPE;
import static org.osc.sdk.manager.Constants.EXTERNAL_SERVICE_NAME;
import static org.osc.sdk.manager.Constants.NOTIFICATION_TYPE;
import static org.osc.sdk.manager.Constants.PROVIDE_DEVICE_STATUS;
import static org.osc.sdk.manager.Constants.SERVICE_NAME;
import static org.osc.sdk.manager.Constants.SYNC_POLICY_MAPPING;
import static org.osc.sdk.manager.Constants.SYNC_SECURITY_GROUP;
import static org.osc.sdk.manager.Constants.VENDOR_NAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.concurrent.GuardedBy;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ApplianceManagerConnectorElementImpl;
import org.osc.core.broker.model.plugin.manager.VirtualSystemElementImpl;
import org.osc.core.broker.model.plugin.sdncontroller.VirtualizationConnectorElementImpl;
import org.osc.core.broker.service.api.plugin.PluginEvent;
import org.osc.core.broker.service.api.plugin.PluginEvent.Type;
import org.osc.core.broker.service.api.plugin.PluginListener;
import org.osc.core.broker.service.api.plugin.PluginService;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.util.ServerUtil;
import org.osc.core.common.controller.ControllerType;
import org.osc.core.common.manager.ManagerType;
import org.osc.core.server.installer.InstallableManager;
import org.osc.sdk.controller.Constants;
import org.osc.sdk.controller.FlowInfo;
import org.osc.sdk.controller.FlowPortInfo;
import org.osc.sdk.controller.Status;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;
import org.osc.sdk.manager.ManagerAuthenticationType;
import org.osc.sdk.manager.ManagerNotificationSubscriptionType;
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
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(immediate = true)
public class ApiFactoryServiceImpl implements ApiFactoryService, PluginService {

    private static final String OSC_PLUGIN_NAME = PluginTracker.PROP_PLUGIN_NAME;
    private final Logger log = Logger.getLogger(ApiFactoryServiceImpl.class);

    private Map<String, ApplianceManagerApi> managerApis = new ConcurrentHashMap<>();
    private Map<String, ComponentServiceObjects<ApplianceManagerApi>> managerRefs = new ConcurrentHashMap<>();
    private Map<String, SdnControllerApi> sdnControllerApis = new ConcurrentHashMap<>();
    private Map<String, ComponentServiceObjects<SdnControllerApi>> sdnControllerRefs = new ConcurrentHashMap<>();

    private List<PluginTracker<?>> pluginTrackers = new LinkedList<>();

    @GuardedBy("pluginListeners")
    private Map<PluginListener, List<PluginTracker<?>>> pluginListeners = new IdentityHashMap<>();

    @GuardedBy("pluginListeners")
    private boolean listenersActive;

    @Reference
    private InstallableManager installableManager;

    @Reference
    private EncryptionApi encrypter;

    @GuardedBy("pluginListeners")
    private BundleContext context;

    @Activate
    void activate(BundleContext context) {
        List<PluginListener> earlyArrivers;
        synchronized (this.pluginListeners) {
            this.context = context;
            this.listenersActive = true;

            earlyArrivers = this.pluginListeners.entrySet().stream().filter(e -> e.getValue().isEmpty())
                    .map(Entry::getKey).collect(Collectors.toList());
        }

        earlyArrivers.stream().forEach(pl -> {
            // Always create trackers without holding any monitors or locks
            // to avoid potential deadlock
            List<PluginTracker<?>> trackers = createTrackers(pl);

            synchronized (this.pluginListeners) {
                if (this.listenersActive) {
                    // We should only add the trackers if the plugin is still
                    // in the map with an empty collection
                    List<PluginTracker<?>> old = this.pluginListeners.get(pl);
                    if (old != null && old.isEmpty()) {
                        this.pluginListeners.put(pl, trackers);
                        trackers = Collections.emptyList();
                    }
                }
            }

            // If our trackers weren't added then close them
            trackers.forEach(PluginTracker::close);
        });

    }

    private List<PluginTracker<?>> createTrackers(PluginListener pl) {
        @SuppressWarnings("rawtypes")
        PluginTrackerCustomizer customizer = pe -> notifyListener(pe, pl);
        @SuppressWarnings("unchecked")
        List<PluginTracker<?>> trackers = Arrays.asList(
                newPluginTracker(customizer));

        return trackers;
    }

    private void notifyListener(org.osc.core.broker.model.plugin.PluginEvent<?> event, PluginListener pl) {
        PluginEvent.Type eventType;
        switch (event.getType()) {
        case ADDING:
            eventType = Type.ADDING;
            break;
        case MODIFIED:
            eventType = Type.MODIFIED;
            break;
        case REMOVED:
            eventType = Type.REMOVED;
            break;
        default:
            this.log.error("Received an unknown plugin event type " + event.getType());
            return;
        }

        PluginEvent toSend = new PluginEvent(eventType, event.getPlugin());
        try {
            pl.pluginEvent(toSend);
        } catch (RuntimeException re) {
            this.log.error("A Plugin listener threw an unexpected exception", re);
        }
    }

    @Deactivate
    void deactivate() {
        synchronized (this.pluginListeners) {
            this.listenersActive = false;
        }
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
            this.log.info("add plugin: " + name);
            refs.put((String) name, serviceObjs);

        } else {
            this.log.warn(String.format("add plugin ignored as %s=%s", OSC_PLUGIN_NAME, name));
        }
    }

    private <T> void removeApi(ComponentServiceObjects<T> serviceObjs, Map<String, ComponentServiceObjects<T>> refs,
            Map<String, T> apis) {
        Object name = serviceObjs.getServiceReference().getProperty(OSC_PLUGIN_NAME);
        if (name instanceof String) {
            this.log.info("remove plugin: " + name);
            refs.remove(name);

            if (apis != null) {
                T service = apis.remove(name);
                if (service != null) {
                    serviceObjs.ungetService(service);
                }
            }
        } else {
            this.log.warn(String.format("remove plugin ignored as %s=%s", OSC_PLUGIN_NAME, name));
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addApplianceManagerApi(ComponentServiceObjects<ApplianceManagerApi> serviceObjs) {
        addApi(serviceObjs, this.managerRefs);
        ManagerType.setTypes(getManagerTypes());
    }

    void removeApplianceManagerApi(ComponentServiceObjects<ApplianceManagerApi> serviceObjs) {
        removeApi(serviceObjs, this.managerRefs, this.managerApis);
        ManagerType.setTypes(getManagerTypes());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addSdnControllerApi(ComponentServiceObjects<SdnControllerApi> serviceObjs) {
        addApi(serviceObjs, this.sdnControllerRefs);
        ControllerType.setTypes(getControllerTypes());
    }

    void removeSdnControllerApi(ComponentServiceObjects<SdnControllerApi> serviceObjs) {
        removeApi(serviceObjs, this.sdnControllerRefs, null);
        ControllerType.setTypes(getControllerTypes());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addPluginListener(PluginListener listener) {
        synchronized (this.pluginListeners) {
            if (!this.listenersActive) {
                // Early joiner waits for startup
                this.pluginListeners.put(listener, emptyList());
                return;
            }
        }

        // Started already - time to join!

        // Always create trackers without holding any monitors or locks
        // to avoid potential deadlock
        List<PluginTracker<?>> trackers = createTrackers(listener);

        // Dynamic references may be set at any time, even while the
        // component is deactivating, therefore we only add the listener
        // if it should be active
        synchronized (this.pluginListeners) {
            if (this.listenersActive) {
                this.pluginListeners.put(listener, trackers);
                trackers = emptyList();
            }
        }

        // If the component was stopped then we may have dangling
        // trackers in this list which should be closed. Otherwise
        // the list will be empty (see above).
        trackers.forEach(PluginTracker::close);
    }

    void removePluginListener(PluginListener listener) {
        List<PluginTracker<?>> trackers;
        synchronized (this.pluginListeners) {
            trackers = this.pluginListeners.remove(listener);
        }

        if (trackers != null) {
            trackers.forEach(PluginTracker::close);
        }
    }

    @Override
    public String generateServiceManagerName(VirtualSystem vs) throws Exception {
        return "OSC "
                + getVendorName(ManagerType
                        .fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()))
                + " " + vs.getDistributedAppliance().getName();
    }

    // Manager Types ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ApplianceManagerApi createApplianceManagerApi(ManagerType managerType) throws Exception {
        return createApplianceManagerApi(managerType.getValue());
    }

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
    public Boolean isPersistedUrlNotifications(ApplianceManagerConnector mc) throws Exception {
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
            shallowClone.setPassword(this.encrypter.decryptAESCTR(shallowClone.getPassword()));
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

    @Override
    public ManagerWebSocketNotificationApi createManagerWebSocketNotificationApi(ApplianceManagerConnector mc)
            throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerWebSocketNotificationApi(getApplianceManagerConnectorElement(mc));
    }

    @Override
    public void checkConnection(ApplianceManagerConnector mc) throws Exception {
        createApplianceManagerApi(mc.getManagerType()).checkConnection(getApplianceManagerConnectorElement(mc));
    }

    @Override
    public ManagerDeviceMemberApi createManagerDeviceMemberApi(ApplianceManagerConnector mc, VirtualSystem vs)
            throws Exception {
        return createApplianceManagerApi(mc.getManagerType()).createManagerDeviceMemberApi(
                getApplianceManagerConnectorElement(mc), new VirtualSystemElementImpl(vs));
    }

    // Controller Types ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public SdnControllerApi createNetworkControllerApi(ControllerType controllerType) throws Exception {
        final String name = controllerType.getValue();
        SdnControllerApi api = this.sdnControllerApis.get(name);

        if (api == null) {
            ComponentServiceObjects<SdnControllerApi> serviceObjs = this.sdnControllerRefs.get(name);
            if (serviceObjs == null) {
                throw new VmidcException(String.format("Sdn plugin not found for controller type: %s", name));
            }
            api = serviceObjs.getService();
            this.sdnControllerApis.put(name, api);
        }
        return api;
    }

    @Override
    public Object getPluginProperty(ControllerType controllerType, String propertyName) throws Exception {
        final String name = controllerType.getValue();
        if (!this.sdnControllerRefs.containsKey(name)) {
            throw new VmidcException("Unsupported Controller type '" + name + "'");
        }
        return this.sdnControllerRefs.get(name).getServiceReference().getProperty(propertyName);
    }

    @Override
    public Set<String> getManagerTypes() {
        return new TreeSet<String>(this.managerRefs.keySet());
    }

    @Override
    public Set<String> getControllerTypes() {
        Set<String> controllerTypes = new TreeSet<>();
        controllerTypes.addAll(this.sdnControllerRefs.keySet());
        return controllerTypes;
    }

    @Override
    public <T> PluginTracker<T> newPluginTracker(PluginTrackerCustomizer<T> customizer) {
        if (customizer == null) {
            throw new IllegalArgumentException("Plugin tracker customizer may not be null");
        }
        PluginTracker<T> tracker = new PluginTracker<>(this.context,
                this.installableManager, customizer);
        synchronized (this.pluginTrackers) {
            this.pluginTrackers.add(tracker);
        }
        tracker.open();
        return tracker;
    }

    @Override
    public boolean usesProviderCreds(String controllerType) throws Exception {
        return (boolean) getPluginProperty(ControllerType.fromText(controllerType), Constants.USE_PROVIDER_CREDS);
    }

    @Override
    public boolean isKeyAuth(String managerType) throws Exception {
        return isKeyAuth(ManagerType.fromText(managerType));
    }

    @Override
    public ManagerDeviceApi createManagerDeviceApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createManagerDeviceApi(getApplianceManagerConnectorElement(vs), new VirtualSystemElementImpl(vs));
    }

    @Override
    public ApplianceManagerConnectorElement getApplianceManagerConnectorElement(VirtualSystem vs)
            throws EncryptionException {
        return getApplianceManagerConnectorElement(vs.getDistributedAppliance().getApplianceManagerConnector());
    }

    @Override
    public ManagerSecurityGroupInterfaceApi createManagerSecurityGroupInterfaceApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createManagerSecurityGroupInterfaceApi(getApplianceManagerConnectorElement(vs),
                        new VirtualSystemElementImpl(vs));
    }

    @Override
    public Boolean providesDeviceStatus(VirtualSystem virtualSystem) throws Exception {
        return providesDeviceStatus(ManagerType
                .fromText(virtualSystem.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    @Override
    public Boolean syncsPolicyMapping(VirtualSystem vs) throws Exception {
        return syncsPolicyMapping(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    @Override
    public String getExternalServiceName(VirtualSystem virtualSystem) throws Exception {
        return getExternalServiceName(
                ManagerType.fromText(virtualSystem.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    @Override
    public Map<String, FlowPortInfo> queryPortInfo(VirtualizationConnector vc, String region,
            HashMap<String, FlowInfo> portsQuery) throws Exception {
        try (SdnControllerApi networkControllerApi = createNetworkControllerApi(vc.getControllerType())) {
            return networkControllerApi.queryPortInfo(getVirtualizationConnectorElement(vc), region, portsQuery);
        }
    }

    @Override
    public SdnControllerApi createNetworkControllerApi(String controllerType) throws Exception {
        return createNetworkControllerApi(ControllerType.fromText(controllerType));
    }

    private VirtualizationConnectorElement getVirtualizationConnectorElement(VirtualizationConnector vc)
            throws Exception {
        VirtualizationConnector shallowClone = new VirtualizationConnector(vc);
        shallowClone.setProviderPassword(this.encrypter.decryptAESCTR(shallowClone.getProviderPassword()));
        if (!StringUtils.isEmpty(shallowClone.getControllerPassword())) {
            shallowClone.setControllerPassword(this.encrypter.decryptAESCTR(shallowClone.getControllerPassword()));
        }
        return new VirtualizationConnectorElementImpl(shallowClone);
    }

    @Override
    public ManagerSecurityGroupApi createManagerSecurityGroupApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createManagerSecurityGroupApi(getApplianceManagerConnectorElement(vs),
                        new VirtualSystemElementImpl(vs));
    }

    @Override
    public ManagerPolicyApi createManagerPolicyApi(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerPolicyApi(getApplianceManagerConnectorElement(mc));
    }

    @Override
    public ManagerDomainApi createManagerDomainApi(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerDomainApi(getApplianceManagerConnectorElement(mc));
    }

    @Override
    public Boolean syncsSecurityGroup(VirtualSystem vs) throws Exception {
        return syncsSecurityGroup(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    @Override
    public ManagerCallbackNotificationApi createManagerUrlNotificationApi(ApplianceManagerConnector mc)
            throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerCallbackNotificationApi(getApplianceManagerConnectorElement(mc));
    }

    @Override
    public SdnRedirectionApi createNetworkRedirectionApi(VirtualSystem vs) throws Exception {
        return createNetworkRedirectionApi(vs.getVirtualizationConnector(), null);
    }

    private SdnRedirectionApi createNetworkRedirectionApi(VirtualizationConnector vc, String region)
            throws Exception {
        SdnControllerApi sca = createNetworkControllerApi(vc.getControllerType());
        return sca.createRedirectionApi(getVirtualizationConnectorElement(vc), region);
    }

    @Override
    public SdnRedirectionApi createNetworkRedirectionApi(VirtualizationConnector vc) throws Exception {
        return createNetworkRedirectionApi(vc, null);
    }

    @Override
    public SdnRedirectionApi createNetworkRedirectionApi(DistributedApplianceInstance dai) throws Exception {
        return createNetworkRedirectionApi(dai.getVirtualSystem(), dai.getDeploymentSpec().getRegion());
    }

    private SdnRedirectionApi createNetworkRedirectionApi(VirtualSystem vs, String region) throws Exception {
        return createNetworkRedirectionApi(vs.getVirtualizationConnector(), region);
    }

    @Override
    public SdnRedirectionApi createNetworkRedirectionApi(SecurityGroupMember sgm) throws Exception {
        return createNetworkRedirectionApi(sgm.getSecurityGroup().getVirtualizationConnector(), getMemberRegion(sgm));
    }

    private String getMemberRegion(SecurityGroupMember sgm) throws VmidcBrokerValidationException {
        switch (sgm.getType()) {
        case VM:
            return sgm.getVm().getRegion();
        case NETWORK:
            return sgm.getNetwork().getRegion();
        case SUBNET:
            return sgm.getSubnet().getRegion();
        default:
            throw new VmidcBrokerValidationException("Openstack Id is not applicable for Members of type '" + sgm.getType()
            + "'");
        }
    }

    @Override
    public Status getStatus(VirtualizationConnector vc, String region) throws Exception {
        try (SdnControllerApi networkControllerApi = createNetworkControllerApi(vc.getControllerType())) {
            return networkControllerApi.getStatus(getVirtualizationConnectorElement(vc), region);
        }
    }

    @Override
    public Boolean supportsOffboxRedirection(VirtualSystem vs) throws Exception {
        return supportsOffboxRedirection(ControllerType.fromText(vs.getVirtualizationConnector().getControllerType()));
    }

    private Boolean supportsOffboxRedirection(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, SUPPORT_OFFBOX_REDIRECTION);
    }

    @Override
    public Boolean supportsOffboxRedirection(SecurityGroup sg) throws Exception {
        return supportsOffboxRedirection(ControllerType.fromText(sg.getVirtualizationConnector().getControllerType()));
    }

    @Override
    public Boolean supportsServiceFunctionChaining(SecurityGroup sg) throws Exception {
        return supportsServiceFunctionChaining(ControllerType.fromText(sg.getVirtualizationConnector().getControllerType()));
    }

    private Boolean supportsServiceFunctionChaining(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, SUPPORT_SFC);
    }

    @Override
    public Boolean supportsFailurePolicy(SecurityGroup sg) throws Exception {
        return supportsFailurePolicy(ControllerType.fromText(sg.getVirtualizationConnector().getControllerType()));
    }

    private Boolean supportsFailurePolicy(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, SUPPORT_FAILURE_POLICY);
    }

    @Override
    public Boolean usesProviderCreds(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, USE_PROVIDER_CREDS);
    }

    @Override
    public Boolean providesTrafficPortInfo(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, QUERY_PORT_INFO);
    }

    @Override
    public Boolean supportsPortGroup(VirtualSystem vs) throws Exception {
        return supportsPortGroup(ControllerType.fromText(vs.getVirtualizationConnector().getControllerType()));
    }

    private Boolean supportsPortGroup(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, SUPPORT_PORT_GROUP);
    }

    @Override
    public Boolean supportsPortGroup(SecurityGroup sg) throws Exception {
        return supportsPortGroup(ControllerType.fromText(sg.getVirtualizationConnector().getControllerType()));
    }

}
