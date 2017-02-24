package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.api.SdnControllerApi;

class VmPortAllHooksRemoveTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(VmPortAllHooksRemoveTask.class);

    private SecurityGroupMember sgm;
    private VMPort port;
    private String sgmName;
    private SecurityGroupMemberType sgmType;

    public VmPortAllHooksRemoveTask(SecurityGroupMember sgm, VMPort port) {
        this.sgm = sgm;
        this.port = port;
        this.sgmType = sgm.getType();
        this.sgmName = sgm.getMemberName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.sgm = (SecurityGroupMember) session.get(SecurityGroupMember.class, this.sgm.getId());
        this.port = (VMPort) session.get(VMPort.class, this.port.getId());

        this.log.info(String.format("Removing hooks for Stale VM Port with MAC '%s' belonging to %s member '%s'",
                this.port.getMacAddresses(), this.sgmType, this.sgmName));

        this.port.removeAllDais();

        SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(this.sgm);
        try {
            controller.removeAllInspectionHooks(this.port);
        } finally {
            controller.close();
        }

        EntityManager.update(session, this.port);
    }

    @Override
    public String getName() {
        return String.format("Removing hooks for Stale Port with MAC '%s' belonging to %s member '%s'",
                this.port.getMacAddresses(), this.sgmType, this.sgmName);
    }

}
