package org.osc.core.server.scheduler;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.util.db.TransactionalRunner;
import org.osc.core.broker.util.db.TransactionalRunner.TransactionalAction;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SyncSecurityGroupJob implements Job {

    private static final Logger log = Logger.getLogger(SyncSecurityGroupJob.class);

    public SyncSecurityGroupJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SessionUtil.setUser(OscAuthFilter.OSC_DEFAULT_LOGIN);
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            EntityManager<SecurityGroup> emgr = new EntityManager<SecurityGroup>(SecurityGroup.class, session);
            Transaction tx = session.beginTransaction();
            List<SecurityGroup> sgs = emgr.listAll();
            tx.commit();

            for (final SecurityGroup sg : sgs) {
                if (sg.getVirtualizationConnector().isVmware()) {
                    continue;
                }
                Thread sgSync = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        new TransactionalRunner<Object, SecurityGroup>(new TransactionalRunner.ExclusiveSessionHandler())
                            .exec(new TransactionalAction<Object, SecurityGroup>() {

                            @Override
                            public Object run(Session session, SecurityGroup sg) {

                                        SessionUtil.setUser(OscAuthFilter.OSC_DEFAULT_LOGIN);

                                try {
                                    sg = (SecurityGroup) session.get(SecurityGroup.class, sg.getId());
                                    ConformService.startSecurityGroupConformanceJob(sg);
                                } catch (Exception ex) {
                                    AlertGenerator.processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE,
                                            new LockObjectReference(sg),
                                            "Failure during scheduling of Security Group Sync. " + ex.getMessage());
                                    log.error("Fail to sync SG " + sg.getName(), ex);
                                }
                                return null;
                            }
                        }, sg);

                    }
                }, "Scheduled-SG-Sync-runner-Thread-" + System.currentTimeMillis());

                sgSync.start();
            }
        } catch (Exception ex) {
            AlertGenerator.processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE, null,
                    "Failure during scheduling of Security Groups Sync. " + ex.getMessage());
            log.error("Fail to get database session or query SGs", ex);

        } finally {

            if (session != null) {
                session.close();
            }
        }
    }

}
