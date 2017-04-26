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
import org.osc.core.broker.service.DeleteDistributedApplianceService;
import org.osc.core.broker.service.SetNATSettingsService;
import org.osc.core.broker.service.SetNetworkSettingsService;
import org.osc.core.broker.service.UpdateUserService;
import org.osc.core.broker.service.UpgradeService;
import org.osc.core.broker.service.api.DeleteUserServiceApi;
import org.osc.core.broker.service.broadcast.Broadcaster;
import org.osc.core.broker.service.mc.AddApplianceManagerConnectorService;
import org.osc.core.broker.service.mc.SyncManagerConnectorService;
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
    private SetNetworkSettingsService setNetworkSettingsService;

    @Reference
    private SetNATSettingsService setNATSettingsService;

    @Reference
    private UpgradeService upgradeService;

    @Reference
    private AddApplianceManagerConnectorService addApplianceManagerConnectorService;

    @Reference
    private UpdateApplianceManagerConnectorService updateApplianceManagerConnectorService;

    @Reference
    private AddVirtualizationConnectorService addVirtualizationConnectorService;

    @Reference
    private UpdateVirtualizationConnectorService updateVirtualizationConnectorService;

    @Reference
    private SyncManagerConnectorService syncManagerConnectorService;

    @Reference
    private DeleteUserServiceApi deleteUserService;

    @Reference
    private UpdateUserService updateUserService;

    @Reference
    private DeleteDistributedApplianceService deleteDistributedApplianceService;

    @Reference
    private Broadcaster broadcaster;

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

    public static SetNetworkSettingsService setNetworkSettingsService() {
        return instance.setNetworkSettingsService;
    }

    public static SetNATSettingsService setNATSettingsService() {
        return instance.setNATSettingsService;
    }

    public static UpgradeService upgradeService() {
        return instance.upgradeService;
    }

    public static AddApplianceManagerConnectorService addApplianceManagerConnectorService() {
        return instance.addApplianceManagerConnectorService;
    }

    public static UpdateApplianceManagerConnectorService updateApplianceManagerConnectorService() {
        return instance.updateApplianceManagerConnectorService;
    }

    public static SyncManagerConnectorService syncManagerConnectorService() {
        return instance.syncManagerConnectorService;
    }

    public static AddVirtualizationConnectorService addVirtualizationConnectorService() {
        return instance.addVirtualizationConnectorService;
    }

    public static UpdateVirtualizationConnectorService updateVirtualizationConnectorService() {
        return instance.updateVirtualizationConnectorService;
    }

    public static DeleteUserServiceApi deleteUserService() {
        return instance.deleteUserService;
    }

    public static DeleteDistributedApplianceService deleteDistributedApplianceService() {
        return instance.deleteDistributedApplianceService;
    }

    public static UpdateUserService updateUserService() {
        return instance.updateUserService;
    }

    public static Broadcaster broadcaster() {
        return instance.broadcaster;
    }

}
