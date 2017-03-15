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

import static org.osc.sdk.controller.Constants.QUERY_PORT_INFO;
import static org.osc.sdk.controller.Constants.SUPPORT_FAILURE_POLICY;
import static org.osc.sdk.controller.Constants.SUPPORT_OFFBOX_REDIRECTION;
import static org.osc.sdk.controller.Constants.SUPPORT_PORT_GROUP;
import static org.osc.sdk.controller.Constants.SUPPORT_SFC;
import static org.osc.sdk.controller.Constants.USE_PROVIDER_CREDS;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.PluginTracker;
import org.osc.core.broker.model.plugin.PluginTrackerCustomizer;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ServiceUnavailableException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.view.maintenance.PluginUploader.PluginType;
import org.osc.core.util.EncryptionUtil;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import com.google.common.collect.ImmutableMap;

public class SdnControllerApiFactory {

    public static final String SDN_CONTROLLER_PLUGINS_DIRECTORY = "sdn_ctrl_plugins";
    private static final Logger log = Logger.getLogger(SdnControllerApiFactory.class);

    private static ApiFactoryService apiFactoryService;
    private static final Map<String, Class<?>> REQUIRED_SDN_CONTROLLER_PLUGIN_PROPERTIES =
            ImmutableMap.<String, Class<?>>builder()
            .put(SUPPORT_OFFBOX_REDIRECTION, Boolean.class)
            .put(SUPPORT_SFC, Boolean.class)
            .put(SUPPORT_FAILURE_POLICY, Boolean.class)
            .put(USE_PROVIDER_CREDS, Boolean.class)
            .put(QUERY_PORT_INFO, Boolean.class)
            .put(SUPPORT_PORT_GROUP, Boolean.class)
            .build();

    private static BundleContext bundleContext;

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
        return createNetworkControllerApi(sgm.getSecurityGroup().getVirtualizationConnector(), getMemberRegion(sgm));
    }

    private static String getMemberRegion(SecurityGroupMember sgm) throws VmidcBrokerValidationException {
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
        sca.setVirtualizationConnector(new VirtualizationConnectorElementImpl(shallowClone));
        sca.setRegion(region);
        return sca;
    }

    public static VMwareSdnApi createVMwareSdnApi(VirtualizationConnector vc) throws VmidcException {
        return apiFactoryService.createVMwareSdnApi(vc);
    }

    public static SdnControllerApi createNetworkControllerApi(String controllerType) throws Exception {
        return createNetworkControllerApi(ControllerType.fromText(controllerType));
    }

    public static SdnControllerApi createNetworkControllerApi(ControllerType controllerType) throws Exception {
        return apiFactoryService.createNetworkControllerApi(controllerType);
    }

    public static <T> PluginTracker<T> newPluginTracker(PluginTrackerCustomizer<T> customizer, Class<T> pluginClass,
            PluginType pluginType) throws ServiceUnavailableException, VmidcException {
        Map<String, Class<?>> requiredProperties = null;
        if (pluginClass == SdnControllerApi.class) {
            requiredProperties = REQUIRED_SDN_CONTROLLER_PLUGIN_PROPERTIES;
        }
        return apiFactoryService.newPluginTracker(customizer, pluginClass, pluginType, requiredProperties);
    }

    public static Boolean supportsOffboxRedirection(VirtualSystem vs) throws Exception {
        return supportsOffboxRedirection(ControllerType.fromText(vs.getVirtualizationConnector().getControllerType()));
    }

    public static Boolean supportsOffboxRedirection(SecurityGroup sg) throws Exception {
        return supportsOffboxRedirection(ControllerType.fromText(sg.getVirtualizationConnector().getControllerType()));
    }

    public static Boolean supportsServiceFunctionChaining(SecurityGroup sg) throws Exception {
        return supportsServiceFunctionChaining(ControllerType.fromText(sg.getVirtualizationConnector().getControllerType()));
    }

    public static Boolean supportsFailurePolicy(SecurityGroup sg) throws Exception {
        return supportsFailurePolicy(ControllerType.fromText(sg.getVirtualizationConnector().getControllerType()));
    }

    public static Boolean usesProviderCreds(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, USE_PROVIDER_CREDS);
    }

    public static Boolean providesTrafficPortInfo(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, QUERY_PORT_INFO);
    }

    public static Boolean supportsPortGroup(VirtualSystem vs) throws Exception {
        return supportsPortGroup(ControllerType.fromText(vs.getVirtualizationConnector().getControllerType()));
    }

    public static Boolean supportsPortGroup(SecurityGroup sg) throws Exception {
        return supportsPortGroup(ControllerType.fromText(sg.getVirtualizationConnector().getControllerType()));
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

    public static Set<String> getControllerTypes() {
        return apiFactoryService.getControllerTypes();
    }

    private static Boolean supportsFailurePolicy(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, SUPPORT_FAILURE_POLICY);
    }

    private static Boolean supportsServiceFunctionChaining(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, SUPPORT_SFC);
    }

    private static Boolean supportsOffboxRedirection(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, SUPPORT_OFFBOX_REDIRECTION);
    }

    private static Boolean supportsPortGroup(ControllerType controllerType) throws Exception {
        return (Boolean) getPluginProperty(controllerType, SUPPORT_PORT_GROUP);
    }

    private static Object getPluginProperty(ControllerType controllerType, String propertyName) throws Exception {
        return apiFactoryService.getPluginProperty(controllerType, propertyName);
    }
}
