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

import java.util.Set;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.manager.api.IscJobNotificationApi;
import org.osc.sdk.manager.api.ManagerCallbackNotificationApi;
import org.osc.sdk.manager.api.ManagerDomainApi;
import org.osc.sdk.manager.api.ManagerPolicyApi;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;
import org.osc.sdk.manager.element.ApplianceManagerConnectorElement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class ManagerApiFactory {
    public static final String MANAGER_PLUGINS_DIRECTORY = "mgr_plugins";
    private static final Logger log = Logger.getLogger(ManagerApiFactory.class);

    private static ApiFactoryService apiFactoryService;
    private static BundleContext bundleContext;

    public static void init() throws Exception {
        bundleContext = FrameworkUtil.getBundle(ManagerApiFactory.class).getBundleContext();

        ServiceTracker<ApiFactoryService, ApiFactoryService> apiFactoryTracker = new ServiceTracker<>(bundleContext,
                ApiFactoryService.class, null);
        apiFactoryTracker.open();

        // TODO: emanoel - Can we use DS here instead?
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

    public static Set<String> getManagerTypes() {
        return apiFactoryService.getManagerTypes();
    }

    private static ApplianceManagerApi createApplianceManagerApi(String managerName) throws Exception {
        return apiFactoryService.createApplianceManagerApi(ManagerType.fromText(managerName));
    }

    public static ManagerSecurityGroupApi createManagerSecurityGroupApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createManagerSecurityGroupApi(getApplianceManagerConnectorElement(vs),
                        new VirtualSystemElementImpl(vs));
    }

    public static ManagerPolicyApi createManagerPolicyApi(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerPolicyApi(getApplianceManagerConnectorElement(mc));
    }

    public static ManagerDomainApi createManagerDomainApi(ApplianceManagerConnector mc) throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerDomainApi(getApplianceManagerConnectorElement(mc));
    }

    public static Boolean syncsSecurityGroup(VirtualSystem vs) throws Exception {
        return apiFactoryService.syncsSecurityGroup(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    public static Boolean providesDeviceStatus(VirtualSystem vs) throws Exception {
        return apiFactoryService.providesDeviceStatus(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    public static Boolean syncsPolicyMapping(ManagerType managerType) throws Exception {
        return apiFactoryService.syncsPolicyMapping(managerType);
    }

    public static Boolean syncsPolicyMapping(VirtualSystem vs) throws Exception {
        return syncsPolicyMapping(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    public static String getExternalServiceName(VirtualSystem vs) throws Exception {
        return apiFactoryService.getExternalServiceName(
                ManagerType.fromText(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType()));
    }

    public static String getServiceName(ManagerType managerType) throws Exception {
        return apiFactoryService.getServiceName(managerType);
    }

    public static ManagerCallbackNotificationApi createManagerUrlNotificationApi(ApplianceManagerConnector mc)
            throws Exception {
        return createApplianceManagerApi(mc.getManagerType())
                .createManagerCallbackNotificationApi(getApplianceManagerConnectorElement(mc));
    }

    public static IscJobNotificationApi createIscJobNotificationApi(VirtualSystem vs) throws Exception {
        return createApplianceManagerApi(vs.getDistributedAppliance().getApplianceManagerConnector().getManagerType())
                .createIscJobNotificationApi(getApplianceManagerConnectorElement(vs), new VirtualSystemElementImpl(vs));
    }

    public static boolean isPersistedUrlNotifications(ApplianceManagerConnector mc) throws Exception {
        return apiFactoryService.isPersistedUrlNotifications(mc);
    }

    public static boolean isWebSocketNotifications(ApplianceManagerConnector mc) throws Exception {
        return apiFactoryService.isWebSocketNotifications(mc);
    }

    public static boolean isBasicAuth(ManagerType mt) throws Exception {
        return apiFactoryService.isBasicAuth(mt);
    }

    public static boolean isKeyAuth(ManagerType mt) throws Exception {
        return apiFactoryService.isKeyAuth(mt);
    }

    private static ApplianceManagerConnectorElement getApplianceManagerConnectorElement(ApplianceManagerConnector mc)
            throws EncryptionException {
        return apiFactoryService.getApplianceManagerConnectorElement(mc);
    }

    private static ApplianceManagerConnectorElement getApplianceManagerConnectorElement(VirtualSystem vs)
            throws EncryptionException {
        return apiFactoryService.getApplianceManagerConnectorElement(vs);
    }

}
