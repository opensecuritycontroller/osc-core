package org.osc.core.server.scheduler;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.sdk.sdn.api.AgentApi;
import org.osc.sdk.sdn.element.AgentElement;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class NsxAgentsJob implements Job {

    private static final Logger LOG = Logger.getLogger(NsxAgentsJob.class);

    public NsxAgentsJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Transaction tx = session.beginTransaction();
            EntityManager<DistributedAppliance> emgr = new EntityManager<DistributedAppliance>(
                    DistributedAppliance.class, session);
            for (DistributedAppliance da : emgr.listAll()) {
                for (VirtualSystem vs : da.getVirtualSystems()) {
                    AgentApi agentApi = VMwareSdnApiFactory.createAgentApi(vs);
                    List<AgentElement> agents = agentApi.getAgents(vs.getNsxServiceId());

                    for (DistributedApplianceInstance dai : vs.getDistributedApplianceInstances()) {
                        AgentElement tgtAgent = null;
                        for (AgentElement agent : agents) {
                            if (agent.getIpAddress() == null) {
                                continue;
                            }
                            if (agent.getIpAddress().equals(dai.getIpAddress())) {
                                tgtAgent = agent;
                                break;
                            }
                        }
                        if (tgtAgent == null) {
                            vs.removeDistributedApplianceInstance(dai);
                            EntityManager.delete(session, dai);
                        } else {
                            dai.setNsxAgentId(tgtAgent.getId());
                            dai.setNsxHostId(tgtAgent.getHostId());
                            dai.setNsxHostName(tgtAgent.getHostName());
                            dai.setNsxVmId(tgtAgent.getVmId());
                            dai.setNsxHostVsmUuid(tgtAgent.getHostVsmId());
                            dai.setMgmtGateway(tgtAgent.getGateway());
                            dai.setMgmtSubnetPrefixLength(tgtAgent.getSubnetPrefixLength());

                            EntityManager.update(session, dai);
                        }
                    }
                }
            }

            tx.commit();
        } catch (Exception ex) {
            LOG.error("Fail to sync DAs", ex);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
