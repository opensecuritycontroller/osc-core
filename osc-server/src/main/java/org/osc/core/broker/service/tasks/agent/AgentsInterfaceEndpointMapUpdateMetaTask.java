package org.osc.core.broker.service.tasks.agent;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.rest.client.agent.model.input.EndpointGroupList;

public class AgentsInterfaceEndpointMapUpdateMetaTask extends TransactionalMetaTask {
    private static final Logger log = Logger.getLogger(AgentsInterfaceEndpointMapUpdateMetaTask.class);

    private String interfaceTag;
    private EndpointGroupList epgl;
    private VirtualSystem vs;
    private TaskGraph tg;

    public AgentsInterfaceEndpointMapUpdateMetaTask(VirtualSystem vs, String interfaceTag, EndpointGroupList epgl) {
        this.vs = vs;
        this.interfaceTag = interfaceTag;
        this.epgl = epgl;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.tg = new TaskGraph();
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

        if (this.vs.getMgrId() == null) {
            log.warn("Manager VSS Device is not yet present. Do nothing.");
            return;
        }

        // Trigger service profile to policy assignment to all DAIs
        for (DistributedApplianceInstance dai : this.vs.getDistributedApplianceInstances()) {
            if (dai.isPolicyMapOutOfSync()) {
                this.tg.addTask(new AgentInterfaceEndpointMapSetTask(dai));
            } else {
                this.tg.addTask(new AgentInterfaceEndpointMapUpdateTask(dai, this.interfaceTag, this.epgl));
            }
        }
    }

    @Override
    public String getName() {
        return "Setting Traffic Policy Mapping '" + this.interfaceTag + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
