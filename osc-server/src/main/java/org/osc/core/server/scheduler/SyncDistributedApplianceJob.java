package org.osc.core.server.scheduler;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.rest.server.VmidcAuthFilter;
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

public class SyncDistributedApplianceJob implements Job {

    private static final Logger log = Logger.getLogger(SyncDistributedApplianceJob.class);

    public SyncDistributedApplianceJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SessionUtil.setUser(VmidcAuthFilter.VMIDC_DEFAULT_LOGIN);
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            EntityManager<DistributedAppliance> emgr = new EntityManager<DistributedAppliance>(
                    DistributedAppliance.class, session);
            Transaction tx = session.beginTransaction();
            List<DistributedAppliance> das = emgr.listAll();
            tx.commit();

            // Iterate on all DAs and execute a sync on a separate thread as we are placing lock and want to avoid
            // delays for following DAs
            for (final DistributedAppliance da : das) {
                Thread daSync = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        new TransactionalRunner<Object, DistributedAppliance>(new TransactionalRunner.ExclusiveSessionHandler())
                            .exec(new TransactionalAction<Object, DistributedAppliance>() {

                            @Override
                            public Object run(Session session, DistributedAppliance da) {

                                SessionUtil.setUser(VmidcAuthFilter.VMIDC_DEFAULT_LOGIN);

                                try {
                                    da = (DistributedAppliance) session.get(DistributedAppliance.class, da.getId());
                                    ConformService.startDAConformJob(session, da, null, false);
                                } catch (Exception ex) {
                                    AlertGenerator.processSystemFailureEvent(
                                            SystemFailureType.SCHEDULER_FAILURE,
                                            new LockObjectReference(da),
                                            "Failure during scheduling of Distributed Appliance Sync. "
                                                    + ex.getMessage());
                                    log.error("Fail to sync DA " + da.getName(), ex);
                                }
                                return null;
                            }
                        }, da);

                    }
                }, "Scheduled-DA-Sync-runner-Thread-" + System.currentTimeMillis());

                daSync.start();

            }

        } catch (Exception ex) {
            AlertGenerator.processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE, null,
                    "Failure during scheduling of Distributed Appliances Sync. " + ex.getMessage());
            log.error("Fail to get database session or query DAs", ex);

        } finally {

            if (session != null) {
                session.close();
            }
        }
    }
}
