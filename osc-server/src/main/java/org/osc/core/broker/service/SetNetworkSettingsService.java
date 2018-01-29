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

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.service.api.SetNetworkSettingsServiceApi;
import org.osc.core.broker.service.dto.NetworkSettingsDto;
import org.osc.core.broker.service.request.SetNetworkSettingsRequest;
import org.osc.core.broker.service.response.SetNetworkSettingsResponse;
import org.osc.core.broker.service.tasks.network.IpChangePropagateMetaTask;
import org.osc.core.broker.service.validator.NetworkSettingsDtoValidator;
import org.osc.core.broker.util.NetworkUtil;
import org.osc.core.broker.util.ValidateUtil;
import org.slf4j.LoggerFactory;
import org.osc.core.server.Server;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

/**
 * This component exposes both the API and the implementation so that the
 * {@link SetNATSettingsService} can call the {@link #startIpPropagateJob()}
 * method. This could be removed if {@link #startIpPropagateJob()} were made
 * part of the {@link SetNetworkSettingsServiceApi}
 */
@Component(service={SetNetworkSettingsService.class, SetNetworkSettingsServiceApi.class})
public class SetNetworkSettingsService extends ServiceDispatcher<SetNetworkSettingsRequest, SetNetworkSettingsResponse>
        implements SetNetworkSettingsServiceApi {

    private static final Logger log = LoggerFactory.getLogger(SetNetworkSettingsService.class);

    @Reference
    private Server server;

    @Reference
    private IpChangePropagateMetaTask ipChangePropagateMetaTask;

    @Override
    public SetNetworkSettingsResponse exec(SetNetworkSettingsRequest request, EntityManager em) throws Exception {

        NetworkSettingsDto networkSettingsDto = new NetworkSettingsDto();
        networkSettingsDto.setDhcp(request.isDhcp());
        boolean isIpChanged = !NetworkUtil.getHostIpAddress().equals(request.getHostIpAddress());

        if(isIpChanged) {
            // If IP is changed, these connections are no longer valid, shutdown so they get restarted again.
            this.server.shutdownRabbitMq();
            this.server.shutdownWebsocket();
        }

        SetNetworkSettingsResponse response = new SetNetworkSettingsResponse();

        /*
         * IP address change needs to get propagated to security managers
         */
        if (isIpChanged) {
            response.setJobId(startIpPropagateJob());
            this.server.startRabbitMq();
            this.server.startWebsocket();
        }

        return response;
    }

    void validate(SetNetworkSettingsRequest req) throws Exception {
        NetworkSettingsDtoValidator.checkForNullFields(req);
        // check for valid IP address format
        ValidateUtil.checkForValidIpAddressFormat(req.getHostIpAddress());
        ValidateUtil.checkForValidIpAddressFormat(req.getHostSubnetMask());
        ValidateUtil.checkForValidIpAddressFormat(req.getHostDefaultGateway());
        if (!req.getHostDnsServer1().isEmpty()) {
            ValidateUtil.checkForValidIpAddressFormat(req.getHostDnsServer1());
        }
        if (!req.getHostDnsServer2().isEmpty()) {
            ValidateUtil.checkForValidIpAddressFormat(req.getHostDnsServer2());
        }

    }

    public Long startIpPropagateJob() throws Exception {

        log.info("Start propagating new IP(" + NetworkUtil.getHostIpAddress() + ") to all managers");

        TaskGraph tg = new TaskGraph();

        tg.addTask(this.ipChangePropagateMetaTask.create());

        Job job = JobEngine.getEngine().submit(
                "Updating " + Server.SHORT_PRODUCT_NAME
                        + " IP to Security Manager(s)", tg, null);
        log.info("Done submitting with jobId: " + job.getId());
        return job.getId();

    }
}
