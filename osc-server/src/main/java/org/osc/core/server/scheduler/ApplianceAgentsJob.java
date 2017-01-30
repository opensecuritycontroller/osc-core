package org.osc.core.server.scheduler;

import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;
import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ApplianceAgentsJob implements Job {

    private static final Logger log = Logger.getLogger(ApplianceAgentsJob.class);

    public ApplianceAgentsJob() {

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
                    for (DistributedApplianceInstance dai : vs.getDistributedApplianceInstances()) {
                        getAgentFullStatus(dai);
                    }
                }
            }

            tx.commit();

        } catch (Exception ex) {

            log.error("Fail to sync DAs", ex);

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private void getAgentFullStatus(DistributedApplianceInstance dai) throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Transaction tx = session.beginTransaction();
            VmidcAgentApi agentApi = new VmidcAgentApi(dai.getIpAddress(), 8090, AgentAuthFilter.VMIDC_AGENT_LOGIN,
                    AgentAuthFilter.VMIDC_AGENT_PASS);

            AgentStatusResponse res = agentApi.getFullStatus();
            dai.setLastStatus(new Date());
            if (res.getAgentDpaInfo() != null && res.getAgentDpaInfo().netXDpaRuntimeInfo != null) {
                dai.setWorkloadInterfaces(res.getAgentDpaInfo().netXDpaRuntimeInfo.workloadInterfaces);
                dai.setPackets(res.getAgentDpaInfo().netXDpaRuntimeInfo.rx);
            }
            EntityManager.update(session, dai);
            tx.commit();

        } catch (Exception ex) {

            log.error("Fail to get full status for dai '" + dai.getName() + "'. " + ex.getMessage());

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
