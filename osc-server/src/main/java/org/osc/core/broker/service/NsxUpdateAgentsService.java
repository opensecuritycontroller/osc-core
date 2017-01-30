package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.nsx.model.Agent;
import org.osc.core.broker.rest.server.api.proprietary.NsxApis.UpdatedAgent;
import org.osc.core.broker.rest.server.api.proprietary.NsxApis.UpdatedAgents;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.NsxUpdateAgentsRequest;
import org.osc.core.broker.service.response.NsxUpdateAgentsResponse;
import org.osc.core.util.NetworkUtil;

public class NsxUpdateAgentsService extends ServiceDispatcher<NsxUpdateAgentsRequest, NsxUpdateAgentsResponse> {

    private static final Logger LOG = Logger.getLogger(NsxUpdateAgentsService.class);

    @Override
    public NsxUpdateAgentsResponse exec(NsxUpdateAgentsRequest request, Session session) throws Exception {

        validate(session, request);

        UpdatedAgents uas = new UpdatedAgents();
        List<UpdatedAgent> ual = new ArrayList<UpdatedAgent>();

        for (Agent agent : request.fabricAgents.list) {

            LOG.info("Agent: " + agent.toString());
            if (agent.allocatedIpAddress == null || agent.allocatedIpAddress.ipAddress == null) {
                continue;
            }
            UpdatedAgent ua = new UpdatedAgent();
            ua.agentId = agent.agentId;
            ua.responseString = new Date().toString();
            ual.add(ua);

            DistributedApplianceInstance dai = DistributedApplianceInstanceEntityMgr.findByNsxAgentIdAndNsxIp(session,
                    agent.agentId, request.nsxIpAddress);

            if (dai != null) {
                AgentRegisterService.updateNsxAgentInfo(session, dai, agent);
            } else {
                String host = NetworkUtil.resolveIpToName(request.nsxIpAddress);
                if (host != null) {
                    dai = DistributedApplianceInstanceEntityMgr.findByNsxAgentIdAndNsxIp(session, agent.agentId, host);
                }
                if (dai != null) {
                    AgentRegisterService.updateNsxAgentInfo(session, dai, agent);
                } else {
                    LOG.warn("Unregistered deployed appliance detected (" + agent + ").");
                }
            }

        }
        uas.updatedAgent = ual;

        NsxUpdateAgentsResponse response = new NsxUpdateAgentsResponse();
        response.updatedAgents = uas;

        return response;
    }

    private void validate(Session session, NsxUpdateAgentsRequest request) throws Exception {

        if (request.fabricAgents == null) {
            throw new VmidcBrokerValidationException("Missing nsx agent list.");
        }

        if (request.nsxIpAddress == null) {
            throw new VmidcBrokerValidationException("Missing nsx IP address.");
        }

        EntityManager<VirtualizationConnector> emgr = new EntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, session);
        VirtualizationConnector vc = emgr.findByFieldName("controllerIpAddress", request.nsxIpAddress);

        if (vc == null) {
            String host = NetworkUtil.resolveIpToName(request.nsxIpAddress);
            if (host != null) {
                vc = emgr.findByFieldName("controllerIpAddress", host);
            }
            if (vc == null) {
                throw new VmidcBrokerValidationException("NSX manager with IP address '" + host + "' not found.");
            }
        }
    }

}
