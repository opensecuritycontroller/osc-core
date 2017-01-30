package org.osc.core.broker.service.request;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

public class DeleteDistributedApplianceRequestValidator implements RequestValidator<BaseDeleteRequest,DistributedAppliance> {

    private Session session;

    public DeleteDistributedApplianceRequestValidator(Session session) {
        this.session = session;
    }

    @Override
    public void validate(BaseDeleteRequest request) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributedAppliance validateAndLoad(BaseDeleteRequest request) throws Exception {
        DistributedAppliance da = (DistributedAppliance) session.get(DistributedAppliance.class, request.getId());

        // entry must pre-exist in db
        if (da == null) { // note: we cannot use name here in error msg since del req does not have name, only ID
            throw new VmidcBrokerValidationException("Distributed Appliance entry with ID '" + request.getId() + "' is not found.");
        }

        if (!da.getMarkedForDeletion() && request.isForceDelete()) {
            throw new VmidcBrokerValidationException(
                    "Distributed Appilance with ID "
                            + request.getId()
                            + " is not marked for deletion and force delete operation is applicable only for entries marked for deletion.");
        }
        return da;
    }
}
