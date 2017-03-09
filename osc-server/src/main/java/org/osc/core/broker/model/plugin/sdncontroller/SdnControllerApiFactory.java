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

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
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

public class SdnControllerApiFactory {

    public static final String SDN_CONTROLLER_PLUGINS_DIRECTORY = "sdn_ctrl_plugins";
    private static final Logger log = Logger.getLogger(SdnControllerApiFactory.class);

    private static ApiFactoryService apiFactoryService;
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
            throw new VmidcBrokerValidationException(
                    "Openstack Id is not applicable for Members of type '" + sgm.getType() + "'");
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
        return apiFactoryService.newPluginTracker(customizer, pluginClass, pluginType);
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
}
