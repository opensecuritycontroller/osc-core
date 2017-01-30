package org.osc.core.broker.service.securityinterface;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.PolicyEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;

public abstract class BaseSecurityGroupInterfaceService<I extends Request, O extends Response> extends
ServiceDispatcher<I, O> {

    protected VirtualSystem vs;
    protected Policy policy;

    protected void validateAndLoad(Session session, SecurityGroupInterfaceDto dto) throws Exception {
        SecurityGroupInterfaceDto.checkForNullFields(dto);
        SecurityGroupInterfaceDto.checkFieldLength(dto);

        this.vs = VirtualSystemEntityMgr.findById(session, dto.getParentId());

        if (this.vs == null || this.vs.getMarkedForDeletion()) {
            throw new VmidcBrokerValidationException("Virtual System with Id: " + dto.getParentId()
            + "  is either not found or is been deleted by the user.");
        }

        boolean isPolicyMappingSupported =
                ManagerApiFactory.createApplianceManagerApi(this.vs).isPolicyMappingSupported();

        if (!isPolicyMappingSupported) {
            throw new VmidcBrokerValidationException("Security group interfaces cannot be created or updated for appliance manager that does not support policy mapping.");
        }

        this.policy = PolicyEntityMgr.findById(session, dto.getPolicyId());

        if (this.policy == null) {
            throw new VmidcBrokerValidationException("Policy with Id: " + dto.getPolicyId() + "  is not found.");
        }

        ApplianceManagerConnector mc = this.vs.getDistributedAppliance().getApplianceManagerConnector();

        if (!mc.getPolicies().contains(this.policy)) {
            throw new VmidcBrokerValidationException("Policy with Name: " + this.policy.getName()
                    + " is not defined in the manager: " + mc.getName());
        }
    }

}
