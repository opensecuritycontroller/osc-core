package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.element.NetworkElement;

public class PortGroupCheckTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(PortGroupCheckTask.class);

    private SecurityGroup sg;
    private SdnControllerApi controller;
    boolean deleteTg;

    public PortGroupCheckTask(SecurityGroup sg, SdnControllerApi controller, boolean deleteTg) {
        this.sg = sg;
        this.deleteTg = deleteTg;
        this.controller = controller;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.sg = (SecurityGroup) session.get(SecurityGroup.class, this.sg.getId());

        Set<SecurityGroupMember> members = this.sg.getSecurityGroupMembers();
        List<NetworkElement> protectedPorts = new ArrayList<>();

        for (SecurityGroupMember sgm : members) {
            protectedPorts.addAll(sgm.getPorts());
        }
        String domainId = OpenstackUtil.extractDomainId(this.sg.getTenantId(), this.sg.getTenantName(),
                this.sg.getVirtualizationConnector(), protectedPorts);
        String portGroupId = this.sg.getNetworkElementId();
        PortGroup portGroup = new PortGroup();
        portGroup.setPortGroupId(portGroupId);

        if (portGroupId != null) {

            if (this.deleteTg) {
                this.controller.deleteNetworkElement(portGroup);
            } else {
                if (portGroup.getParentId() == null) {
                    portGroup.setParentId(domainId);
                }
                for (NetworkElement elem : protectedPorts) {
                    if (elem.getParentId() == null) {
                        ((VMPort) elem).setParentId(domainId);
                    }
                }
                NetworkElement pGrp = this.controller.updateNetworkElement(portGroup, protectedPorts);
                if (pGrp != null && !pGrp.getElementId().equals(portGroup.getElementId())) {
                    //portGroup was deleted outside OSC, recreated portGroup above
                    this.sg.setNetworkElementId(pGrp.getElementId());
                    session.update(this.sg);
                }
            }
        } else {
            if (CollectionUtils.isNotEmpty(protectedPorts)) {
                for (NetworkElement vmPort : protectedPorts) {
                    ((VMPort) vmPort).setParentId(domainId);
                }
                NetworkElement portGp = this.controller.registerNetworkElement(protectedPorts);
                if (portGp != null) {
                    this.sg.setNetworkElementId(portGp.getElementId());
                    session.update(this.sg);
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Port Group '%s' members", this.sg.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sg);
    }
}
