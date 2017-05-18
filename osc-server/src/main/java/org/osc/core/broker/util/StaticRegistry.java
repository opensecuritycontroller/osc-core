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

import org.osc.core.broker.service.NsxUpdateAgentsService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.appliance.UploadConfig;
import org.osc.core.broker.service.broadcast.Broadcaster;
import org.osc.core.server.Server;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

/**
 * This registry is a work-around to temporarily allow some static calls to remain after they have been removed from the
 * REST API.
 *
 * The methods in this class should <b>not</b> be called from static initialisers, as this class is not initialised at
 * static initialisation time; instead they should be called from static methods.
 */
@Component(service = StaticRegistry.class, immediate = true,
    configurationPid="org.osc.core.broker.upload",
    configurationPolicy=ConfigurationPolicy.REQUIRE)
@Deprecated
public class StaticRegistry {

    @Reference
    private Server server;

    @Reference
    private Broadcaster broadcaster;

    @Reference
    private EncryptionApi encryptionApi;

    @Reference
    private TransactionalBroadcastUtil txBroadcastUtil;

    @Reference
    private AlertGenerator alertGenerator;

    @Reference
    private NsxUpdateAgentsService nsxUpdateAgentsService;

    private String uploadPath;

    private static StaticRegistry instance = null;

    @Activate
    void activate(UploadConfig config) {
        this.uploadPath = config.upload_path();
        instance = this;
    }

    public static Server server() {
        return instance.server;
    }

    public static Broadcaster broadcaster() {
        return instance.broadcaster;
    }

    public static String uploadPath() {
        return instance.uploadPath;
    }

    public static EncryptionApi encryptionApi() {
        return instance.encryptionApi;
    }

    public static TransactionalBroadcastUtil transactionalBroadcastUtil() {
        return instance.txBroadcastUtil;
    }

    public static AlertGenerator alertGenerator() {
        return instance.alertGenerator;
    }

    public static NsxUpdateAgentsService nsxUpdateAgentsService() {
        return instance.nsxUpdateAgentsService;
    }
}
