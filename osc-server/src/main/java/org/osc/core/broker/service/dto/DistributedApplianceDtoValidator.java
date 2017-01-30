package org.osc.core.broker.service.dto;

import java.util.HashMap;
import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.util.ValidateUtil;

public class DistributedApplianceDtoValidator implements DtoValidator<DistributedApplianceDto, DistributedAppliance> {
    private Session session;
    private final static String ENCAPSULATION_TYPE_FIELD = "Encapsulation Type";
    private final static String DOMAIN_ID_FIELD = "Domain Id";

    public DistributedApplianceDtoValidator(Session session) {
        this.session = session;
    }

    @Override
    public void validateForCreate(DistributedApplianceDto dto) throws Exception {
        validate(dto);

        EntityManager<DistributedAppliance> emgr = new EntityManager<DistributedAppliance>(DistributedAppliance.class,
                this.session);

        if (emgr.isExisting("name", dto.getName())) {
            throw new VmidcBrokerValidationException("Distributed Appliance Name: " + dto.getName()
            + " already exists.");
        }
    }

    @Override
    public DistributedAppliance validateForUpdate(DistributedApplianceDto dto) throws Exception {
        BaseDto.checkForNullId(dto);

        DistributedAppliance da = (DistributedAppliance) this.session.get(DistributedAppliance.class, dto.getId());

        if (da == null) {

            throw new VmidcBrokerValidationException("Distributed Appliance entry with name: "
                    + dto.getName() + ") is not found.");
        }

        ValidateUtil.checkMarkedForDeletion(da, da.getName());

        if (!dto.getMcId().equals(da.getApplianceManagerConnector().getId())) {

            throw new VmidcBrokerValidationException("Appliance Manager Connector change is not allowed.");
        }

        validate(dto, false);

        return da;
    }

    //TODO Emanoel Add a unit test for image url update for DA
    void validate(DistributedApplianceDto dto) throws Exception {
        validate(dto, true);
    }

    void validate(DistributedApplianceDto dto, boolean forCreate) throws Exception {
        DistributedApplianceDto.checkForNullFields(dto);

        // Ensure DA name complies with Manager naming requirements
        if (!ValidateUtil.validateDaName(dto.getName())) {
            throw new VmidcBrokerValidationException(
                    "Invalid Distributed Appliance Name: "
                            + dto.getName()
                            + "DA name must not exceed 13 characters, must start with a letter, and can only contain numbers, letters and dash(-).");
        }

        DistributedApplianceDto.checkFieldLength(dto);

        Set<VirtualSystemDto> vsDtoList = dto.getVirtualizationSystems();

        if (vsDtoList == null || vsDtoList.size() == 0) {
            throw new VmidcBrokerValidationException(
                    "The associated Virtualization System must be selected for this Distributed Appliance.");
        }

        Appliance a = ApplianceEntityMgr.findById(this.session, dto.getApplianceId());

        if (a == null) {
            throw new VmidcBrokerValidationException(
                    "The associated Appliance must be selected for this Distributed Appliance.");
        }

        EntityManager<ApplianceManagerConnector> mcMgr = new EntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, this.session);
        ApplianceManagerConnector mc = mcMgr.findByPrimaryKey(dto.getMcId());

        if (mc == null) {
            throw new VmidcBrokerValidationException(
                    "The associated Appliance Manager Connector must be selected for this Distributed Appliance.");
        }

        for (VirtualSystemDto vsDto : dto.getVirtualizationSystems()) {
            VirtualSystemDto.checkForNullFields(vsDto);

            VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(this.session, vsDto.getVcId());

            if (vc == null) {
                throw new VmidcBrokerValidationException(
                        "Distributed Appliance using the associated Virtualization Connector with Id: "
                                + vsDto.getVcId() + " does not exist.");
            }

            HashMap<String, Object> nullFields = new HashMap<>();
            HashMap<String, Object> notNullFields = new HashMap<>();

            boolean isPolicyMappingSupported =
                    ManagerApiFactory.createApplianceManagerApi(mc.getManagerType()).isPolicyMappingSupported();

            if (isPolicyMappingSupported) {
                if (vc.getVirtualizationType() == VirtualizationType.OPENSTACK) {
                    notNullFields.put(ENCAPSULATION_TYPE_FIELD, vsDto.getEncapsulationType());
                } else {
                    nullFields.put(ENCAPSULATION_TYPE_FIELD, vsDto.getEncapsulationType());
                }

                notNullFields.put(DOMAIN_ID_FIELD, vsDto.getDomainId());
            } else {
                nullFields.put(ENCAPSULATION_TYPE_FIELD, vsDto.getEncapsulationType());
                nullFields.put(DOMAIN_ID_FIELD, vsDto.getDomainId());
            }

            ValidateUtil.checkForNullFields(notNullFields);
            ValidateUtil.validateFieldsAreNull(nullFields);

            ApplianceSoftwareVersion av = ApplianceSoftwareVersionEntityMgr.findByApplianceVersionVirtTypeAndVersion(this.session,
                    dto.getApplianceId(), dto.getApplianceSoftwareVersionName(), vc.getVirtualizationType(),
                    vc.getVirtualizationSoftwareVersion());

            if (av == null) {
                throw new VmidcBrokerValidationException(
                        "Incompatible Distributed Appliance and The associated Appliance Software Version.");
            }

            if (forCreate && VirtualSystemEntityMgr.findByDAAndVC(this.session, dto.getId(), vsDto.getVcId()) != null) {
                throw new VmidcBrokerValidationException(
                        "The composite key Distributed Appliance, Virtualization Connector already exists.");
            }

            if (isPolicyMappingSupported) {
                EntityManager<Domain> em = new EntityManager<Domain>(Domain.class, this.session);
                Domain domain = em.findByPrimaryKey(vsDto.getDomainId());

                if (domain == null) {
                    throw new VmidcBrokerValidationException("Domain used in Virtual System " + vc.getName()
                    + " cannot be found.");
                }

                if (domain.getName().length() > ValidateUtil.DEFAULT_MAX_LEN) {
                    throw new VmidcBrokerInvalidEntryException("Invalid domain length found in Virtual System "
                            + vc.getName() + ".");
                }
            }
        }
    }
}