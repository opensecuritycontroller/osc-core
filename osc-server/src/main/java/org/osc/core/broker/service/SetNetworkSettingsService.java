package org.osc.core.broker.service;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.service.dto.NetworkSettingsDto;
import org.osc.core.broker.service.request.SetNetworkSettingsRequest;
import org.osc.core.broker.service.response.SetNetworkSettingsResponse;
import org.osc.core.broker.service.tasks.network.IpChangePropagateMetaTask;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.util.network.NetworkSettingsApi;
import org.osc.core.util.NetworkUtil;

import com.mcafee.vmidc.server.Server;

public class SetNetworkSettingsService extends ServiceDispatcher<SetNetworkSettingsRequest, SetNetworkSettingsResponse> {

    private static final Logger log = Logger.getLogger(SetNetworkSettingsService.class);

    @Override
    public SetNetworkSettingsResponse exec(SetNetworkSettingsRequest request, Session session) throws Exception {

        NetworkSettingsApi networkSettingsApi = new NetworkSettingsApi();
        NetworkSettingsDto networkSettingsDto = new NetworkSettingsDto();
        networkSettingsDto.setDhcp(request.isDhcp());
        if (!request.isDhcp()) {
            networkSettingsDto.setHostIpAddress(request.getHostIpAddress());
            networkSettingsDto.setHostSubnetMask(request.getHostSubnetMask());
            networkSettingsDto.setHostDefaultGateway(request.getHostDefaultGateway());
            networkSettingsDto.setHostDnsServer1(request.getHostDnsServer1());
            networkSettingsDto.setHostDnsServer2(request.getHostDnsServer2());
            validate(request);
        }
        boolean isIpChanged = !NetworkUtil.getHostIpAddress().equals(request.getHostIpAddress());

        if(isIpChanged) {
            // If IP is changed, these connections are no longer valid, shutdown so they get restarted again.
            Server.shutdownRabbitMq();
            Server.shutdownWebsocket();
        }

        networkSettingsApi.setNetworkSettings(networkSettingsDto);
        SetNetworkSettingsResponse response = new SetNetworkSettingsResponse();

        /*
         * IP address change needs to get propagated to NSX managers and DAIs
         */
        if (isIpChanged) {
            response.setJobId(startIpPropagateJob());
            Server.startRabbitMq();
            Server.startWebsocket();
        }

        return response;
    }

    void validate(SetNetworkSettingsRequest req) throws Exception {
        NetworkSettingsDto.checkForNullFields(req);
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

    public static Long startIpPropagateJob() throws Exception {

        log.info("Start propagating new IP(" + NetworkUtil.getHostIpAddress() + ") to all NSX managers and DAIs");

        TaskGraph tg = new TaskGraph();

        tg.addTask(new IpChangePropagateMetaTask());

        Job job = JobEngine.getEngine().submit(
                "Updating " + Server.SHORT_PRODUCT_NAME
                        + " IP to Appliance Instance Agent(s), Element Manager(s) and NSX Manager(s)", tg, null);
        log.info("Done submitting with jobId: " + job.getId());
        return job.getId();

    }
}
