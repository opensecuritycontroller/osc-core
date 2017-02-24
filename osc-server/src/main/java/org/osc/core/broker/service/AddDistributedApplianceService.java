package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.DistributedApplianceDtoValidator;
import org.osc.core.broker.service.dto.DtoValidator;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.AddDistributedApplianceResponse;
import org.osc.core.util.PKIUtil;

public class AddDistributedApplianceService extends
ServiceDispatcher<BaseRequest<DistributedApplianceDto>, AddDistributedApplianceResponse> {

    private DtoValidator<DistributedApplianceDto, DistributedAppliance> validator;

    @Override
    public AddDistributedApplianceResponse exec(BaseRequest<DistributedApplianceDto> request, Session session)
            throws Exception {

        EntityManager<ApplianceManagerConnector> mcMgr = new EntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, session);

        DistributedApplianceDto daDto = request.getDto();

        if (this.validator == null) {
            this.validator = new DistributedApplianceDtoValidator(session);
        }

        this.validator.validateForCreate(daDto);

        ApplianceManagerConnector mc = mcMgr.findByPrimaryKey(daDto.getMcId());
        Appliance a = ApplianceEntityMgr.findById(session, daDto.getApplianceId());

        DistributedAppliance da = new DistributedAppliance(mc);

        List<VirtualSystem> vsList = getVirtualSystems(session, request.getDto(), da);

        // creating new entry in the db using entity manager object
        DistributedApplianceEntityMgr.createEntity(session, request.getDto(), a, da);
        EntityManager.create(session, da);

        for (VirtualSystem vs : vsList) {
            EntityManager.create(session, vs);
            // Ensure name field is populated is assigned after DB id had been allocated
            vs.getName();
            da.addVirtualSystem(vs);
        }

        AddDistributedApplianceResponse response = new AddDistributedApplianceResponse();
        DistributedApplianceEntityMgr.fromEntity(da, response);
        if(request.isApi()) {
            response.setSecretKey(null);
        }

        commitChanges(true);

        Long jobId = startConformDAJob(da, session);

        response.setJobId(jobId);

        return response;
    }

    private Long startConformDAJob(DistributedAppliance da, Session session) throws Exception {
        return ConformService.startDAConformJob(session, da);
    }

    List<VirtualSystem> getVirtualSystems(Session session, DistributedApplianceDto daDto, DistributedAppliance da) throws Exception {
        List<VirtualSystem> vsList = new ArrayList<VirtualSystem>();

        // build the list of associated VirtualSystems for this DA
        Set<VirtualSystemDto> vsDtoList = daDto.getVirtualizationSystems();

        for (VirtualSystemDto vsDto : vsDtoList) {
            VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(session, vsDto.getVcId());

            // load the corresponding app sw version from db
            ApplianceSoftwareVersion av = ApplianceSoftwareVersionEntityMgr.findByApplianceVersionVirtTypeAndVersion(session,
                    daDto.getApplianceId(), daDto.getApplianceSoftwareVersionName(), vc.getVirtualizationType(),
                    vc.getVirtualizationSoftwareVersion());

            EntityManager<Domain> em = new EntityManager<Domain>(Domain.class, session);
            Domain domain = vsDto.getDomainId() == null ? null : em.findByPrimaryKey(vsDto.getDomainId());

            VirtualSystem vs = new VirtualSystem(da);

            vs.setApplianceSoftwareVersion(av);
            vs.setDomain(domain);
            vs.setVirtualizationConnector(vc);
            vs.setEncapsulationType(vsDto.getEncapsulationType());
            // generate key store and persist it as byte array in db
            vs.setKeyStore(PKIUtil.generateKeyStore());
            vsList.add(vs);
        }

        return vsList;
    }
}
