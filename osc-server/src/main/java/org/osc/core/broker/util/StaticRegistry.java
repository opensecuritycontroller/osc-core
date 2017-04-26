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
package org.osc.core.broker.util;

import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.api.AcknowledgeAlertServiceApi;
import org.osc.core.broker.service.api.AddAlarmServiceApi;
import org.osc.core.broker.service.broadcast.Broadcaster;
import org.osc.core.broker.service.mc.AddApplianceManagerConnectorService;
import org.osc.core.broker.service.mc.UpdateApplianceManagerConnectorService;
import org.osc.core.broker.service.vc.AddVirtualizationConnectorService;
import org.osc.core.broker.service.vc.UpdateVirtualizationConnectorService;
import org.osc.core.server.Server;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This registry is a work-around to temporarily allow some static calls to remain after they have been removed from the
 * REST API.
 *
 * The methods in this class should <b>not</b> be called from static initialisers, as this class is not initialised at
 * static initialisation time; instead they should be called from static methods.
 */
@Component(service = StaticRegistry.class, immediate = true)
@Deprecated
public class StaticRegistry {

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private Server server;

    @Reference
    private ConformService conformService;

    @Reference
    private AddApplianceManagerConnectorService addApplianceManagerConnectorService;

    @Reference
    private UpdateApplianceManagerConnectorService updateApplianceManagerConnectorService;

    @Reference
    private AddVirtualizationConnectorService addVirtualizationConnectorService;

    @Reference
    private UpdateVirtualizationConnectorService updateVirtualizationConnectorService;

    @Reference
    private Broadcaster broadcaster;

    @Reference
    private AcknowledgeAlertServiceApi acknowledgeAlertServiceApi;

    @Reference
    private AddAlarmServiceApi addAlarmServiceApi;

    private static StaticRegistry instance = null;

    @Activate
    void activate() {
        instance = this;
    }

    public static ApiFactoryService apiFactoryService() {
        return instance.apiFactoryService;
    }

    public static Server server() {
        return instance.server;
    }

    public static ConformService conformService() {
        return instance.conformService;
    }

    public static AddApplianceManagerConnectorService addApplianceManagerConnectorService() {
        return instance.addApplianceManagerConnectorService;
    }

    public static UpdateApplianceManagerConnectorService updateApplianceManagerConnectorService() {
        return instance.updateApplianceManagerConnectorService;
    }

    public static AddVirtualizationConnectorService addVirtualizationConnectorService() {
        return instance.addVirtualizationConnectorService;
    }

    public static UpdateVirtualizationConnectorService updateVirtualizationConnectorService() {
        return instance.updateVirtualizationConnectorService;
    }

    public static Broadcaster broadcaster() {
        return instance.broadcaster;
    }

    public static AcknowledgeAlertServiceApi acknowledgeAlertServiceApi() {
        return instance.acknowledgeAlertServiceApi ;
    }

    public static AddAlarmServiceApi addAlarmServiceApi() {
        return instance.addAlarmServiceApi;
    }
}
