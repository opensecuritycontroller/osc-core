package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;



public class DeleteApplianceService extends ServiceDispatcher<BaseIdRequest, EmptySuccessResponse> {

    @Override
    public EmptySuccessResponse exec(BaseIdRequest request, Session session) throws Exception {

        validate(session, request);

        // Initializing Entity Manager
        EntityManager<Appliance> emgr = new EntityManager<Appliance>(Appliance.class, session);

        emgr.delete(request.getId());

        EmptySuccessResponse response = new EmptySuccessResponse();
        return response;
    }

    void validate(Session session, BaseIdRequest request) throws Exception {

        Appliance a = (Appliance) session.get(Appliance.class, request.getId());

        // entry must pre-exist in db
        if (a == null) { // note: we cannot use name here in error msg since del
                         // req does not have name, only ID

            throw new VmidcBrokerValidationException("Appliance with Id " + request.getId() + " is not found.");
        }

        if (ApplianceSoftwareVersionEntityMgr.isReferencedByApplianceSoftwareVersion(session, a)) {

            throw new VmidcBrokerInvalidRequestException(
                    "Cannot delete an Appliance that has associated Appliance Software Versions.");
        }
    }

}
