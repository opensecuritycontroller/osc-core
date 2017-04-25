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
package org.osc.core.broker.service;

import java.io.File;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.UpgradeServiceApi;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.UpgradeRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.server.Server;
import org.osc.core.util.ServerUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = UpgradeService.class)
public class UpgradeService extends ServiceDispatcher<UpgradeRequest, EmptySuccessResponse> implements UpgradeServiceApi {
    private static final Logger log = Logger.getLogger(UpgradeService.class);

    @Reference
    private Server server;

    @Override
    public EmptySuccessResponse exec(UpgradeRequest request, EntityManager em) throws Exception {
        File uploadedFile = request.getUploadedFile();
        log.info("Upgrade Req (pid:" + ServerUtil.getCurrentPid() + "): uploaded File: "
                + uploadedFile.getCanonicalPath());

        try {
            this.server.setInMaintenance(true);
            ServerUtil.upgradeServer(uploadedFile);
        } catch (Exception e) {
            this.server.setInMaintenance(false);
            throw new VmidcException("Upgrade failed: " + e);
        } finally {
            uploadedFile.delete();
        }

        return new EmptySuccessResponse();
    }

}
