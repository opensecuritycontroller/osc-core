package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.MgrSecurityGroupInterfacesCheckMetaTask;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.sdk.manager.api.ApplianceManagerApi;

public class SecurityGroupCheckMetaTask extends TransactionalMetaTask {

    private SecurityGroup sg;
    private TaskGraph tg;

    public SecurityGroupCheckMetaTask(SecurityGroup sg) {
        this.sg = sg;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.sg = (SecurityGroup) session.get(SecurityGroup.class, this.sg.getId());

        this.tg = new TaskGraph();
        this.tg.addTask(new ValidateSecurityGroupTenantTask(this.sg));
        this.tg.appendTask(new SecurityGroupUpdateOrDeleteMetaTask(this.sg));

        if (!ValidateUtil.isEmpty(this.sg.getSecurityGroupInterfaces())) {
            List<VirtualSystem> referencedVs = VirtualSystemEntityMgr.listReferencedVSBySecurityGroup(session,
                    this.sg.getId());
            for (VirtualSystem vs : referencedVs) {
                ApplianceManagerApi applianceManagerApi = ManagerApiFactory.createApplianceManagerApi(vs);
                if (vs.getMgrId() != null && applianceManagerApi.isPolicyMappingSupported()) {
                    this.tg.appendTask(new MgrSecurityGroupInterfacesCheckMetaTask(vs),
                            TaskGuard.ALL_PREDECESSORS_COMPLETED);
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Security Group '%s'", this.sg.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sg);
    }

}
