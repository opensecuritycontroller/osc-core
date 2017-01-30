package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.rest.client.nsx.model.ServiceProfile;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

public class NsxServiceProfileCheckMetaTask extends TransactionalMetaTask {
    private static final Logger log = Logger.getLogger(NsxServiceProfileCheckMetaTask.class);

    private VirtualSystem vs;
    private ServiceProfile serviceProfile;
    private TaskGraph tg;

    public NsxServiceProfileCheckMetaTask(VirtualSystem vs, ServiceProfile serviceProfile) {
        this.vs = vs;
        this.serviceProfile = serviceProfile;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.info("Start executing ServiceProfileCheckMetaTask task for VS '" + vs.getId() + "'");
        tg = new TaskGraph();

        vs = (VirtualSystem) session.get(VirtualSystem.class, vs.getId());

        NsxSecurityGroupInterfacesCheckMetaTask.processServiceProfile(session, tg, vs, serviceProfile);
    }

    @Override
    public String getName() {
        return "Checking Service Profile '" + serviceProfile.getName() + "' on Virtual System '"
                + vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(vs);
    }

}
