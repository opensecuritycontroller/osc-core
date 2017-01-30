package org.osc.core.broker.rest.server.model;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.RequestValidator;

public class TagVmRequestValidator implements RequestValidator<TagVmRequest, DistributedApplianceInstance> {

    private Session session;

    public TagVmRequestValidator(Session session) {
        this.session = session;
    }

    @Override
    public void validate(TagVmRequest request) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributedApplianceInstance validateAndLoad(TagVmRequest request) throws Exception {
        if (request == null || request.getApplianceInstanceName() == null || request.getApplianceInstanceName().isEmpty()) {
            throw new VmidcBrokerValidationException("Null request or invalid Appliance Instance Name.");
        }

        EntityManager<DistributedApplianceInstance> emgr = new EntityManager<>(DistributedApplianceInstance.class, session);
        DistributedApplianceInstance dai = emgr.findByFieldName("name", request.getApplianceInstanceName());

        if (dai == null) {
            throw new VmidcBrokerValidationException("Appliance Instance Name '" + request.getApplianceInstanceName() + "' not found.");
        }

        return dai;
    }
}
