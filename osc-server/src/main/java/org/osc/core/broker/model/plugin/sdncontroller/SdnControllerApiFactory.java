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

import static org.osc.sdk.controller.Constants.*;

import java.util.HashMap;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ServiceUnavailableException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.sdk.controller.FlowInfo;
import org.osc.sdk.controller.FlowPortInfo;
import org.osc.sdk.controller.Status;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;
import org.osc.sdk.sdn.api.VMwareSdnApi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class SdnControllerApiFactory {

    public static final String SDN_CONTROLLER_PLUGINS_DIRECTORY = "sdn_ctrl_plugins";
    private static final Logger log = Logger.getLogger(SdnControllerApiFactory.class);

    private static ApiFactoryService apiFactoryService;

    private static BundleContext bundleContext;

    public static SdnRedirectionApi createNetworkRedirectionApi(VirtualSystem vs) throws Exception {
        return createNetworkRedirectionApi(vs.getVirtualizationConnector(), null);
    }

    public static SdnRedirectionApi createNetworkRedirectionApi(VirtualizationConnector vc) throws Exception {
        return createNetworkRedirectionApi(vc, null);
    }

    public static SdnRedirectionApi createNetworkRedirectionApi(DistributedApplianceInstance dai) throws Exception {
        return createNetworkRedirectionApi(dai.getVirtualSystem(), dai.getDeploymentSpec().getRegion());
    }

    public static SdnRedirectionApi createNetworkRedirectionApi(SecurityGroupMember sgm) throws Exception {
        return createNetworkRedirectionApi(sgm.getSecurityGroup().getVirtualizationConnector(), getMemberRegion(sgm));
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

    public static SdnRedirectionApi createNetworkRedirectionApi(VirtualSystem vs, String region) throws Exception {
        return createNetworkRedirectionApi(vs.getVirtualizationConnector(), region);
    }

    private static SdnRedirectionApi createNetworkRedirectionApi(VirtualizationConnector vc, String region)
            throws Exception {
        SdnControllerApi sca = createNetworkControllerApi(vc.getControllerType());
        return sca.createRedirectionApi(getVirtualizationConnectorElement(vc), region);
    }

    private static VirtualizationConnectorElement getVirtualizationConnectorElement(VirtualizationConnector vc)
            throws Exception {
        VirtualizationConnector shallowClone = new VirtualizationConnector(vc);
        shallowClone.setProviderPassword(StaticRegistry.encryptionApi().decryptAESCTR(shallowClone.getProviderPassword()));
        if (!StringUtils.isEmpty(shallowClone.getControllerPassword())) {
            shallowClone.setControllerPassword(StaticRegistry.encryptionApi().decryptAESCTR(shallowClone.getControllerPassword()));
        }
        return new VirtualizationConnectorElementImpl(shallowClone);
    }

    public static VMwareSdnApi createVMwareSdnApi(VirtualizationConnector vc) throws VmidcException {
        return apiFactoryService.createVMwareSdnApi(vc);
    }

    private static SdnControllerApi createNetworkControllerApi(String controllerType) throws Exception {
        return createNetworkControllerApi(ControllerType.fromText(controllerType));
    }

    private static SdnControllerApi createNetworkControllerApi(ControllerType controllerType) throws Exception {
        return apiFactoryService.createNetworkControllerApi(controllerType);
    }

    public static Status getStatus(VirtualizationConnector vc, String region) throws Exception {
        try (SdnControllerApi networkControllerApi = createNetworkControllerApi(vc.getControllerType())) {
            return networkControllerApi.getStatus(getVirtualizationConnectorElement(vc), region);
        }
    }

    public static HashMap<String, FlowPortInfo> queryPortInfo(VirtualizationConnector vc, String region,
            HashMap<String, FlowInfo> portsQuery) throws Exception {
        try (SdnControllerApi networkControllerApi = createNetworkControllerApi(vc.getControllerType())) {
            return networkControllerApi.queryPortInfo(getVirtualizationConnectorElement(vc), region, portsQuery);
        }
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

        // TODO: emanoel - Can DS be used here?
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
