package org.osc.core.broker.service;

import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.request.ListEncapsulationTypeByVersionTypeAndModelRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.sdk.controller.TagEncapsulationType;

public class ListEncapsulationTypeByVersionTypeAndModel extends
        ServiceDispatcher<ListEncapsulationTypeByVersionTypeAndModelRequest, ListResponse<TagEncapsulationType>> {

    ListResponse<TagEncapsulationType> response = new ListResponse<>();

    @Override
    public ListResponse<TagEncapsulationType> exec(ListEncapsulationTypeByVersionTypeAndModelRequest request,
            Session session) throws Exception {
        List<TagEncapsulationType> list = ApplianceSoftwareVersionEntityMgr.getEncapsulationByApplianceSoftwareVersion(
                session, request.getAppliacneSoftwareVersion(), request.getAppliacneModel(), request.getVcType());
        this.response.setList(list);

        return this.response;
    }
}
