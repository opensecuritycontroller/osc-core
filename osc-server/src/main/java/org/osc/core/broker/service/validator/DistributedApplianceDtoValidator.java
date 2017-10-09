/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DistributedApplianceDtoValidator.class)
public class DistributedApplianceDtoValidator implements DtoValidator<DistributedApplianceDto, DistributedAppliance> {
    private EntityManager em;
    private final static String ENCAPSULATION_TYPE_FIELD = "Encapsulation Type";
    private final static String DOMAIN_ID_FIELD = "Domain Id";
    private TransactionalBroadcastUtil txBroadcastUtil;

    @Reference
    ApiFactoryService apiFactoryService;

    public DistributedApplianceDtoValidator create(EntityManager em) {
        DistributedApplianceDtoValidator validator = new DistributedApplianceDtoValidator();
        validator.em = em;
        validator.apiFactoryService = this.apiFactoryService;
        return validator;
    }

    @Override
    public void validateForCreate(DistributedApplianceDto dto) throws Exception {
        validate(dto);

        OSCEntityManager<DistributedAppliance> emgr = new OSCEntityManager<DistributedAppliance>(DistributedAppliance.class,
                this.em, this.txBroadcastUtil);

        if (emgr.isExisting("name", dto.getName())) {
            throw new VmidcBrokerValidationException("Distributed Appliance Name: " + dto.getName()
            + " already exists.");
        }
    }

    @Override
    public DistributedAppliance validateForUpdate(DistributedApplianceDto dto) throws Exception {
        BaseDtoValidator.checkForNullId(dto);

        DistributedAppliance da = this.em.find(DistributedAppliance.class, dto.getId());

        if (da == null) {

            throw new VmidcBrokerValidationException("Distributed Appliance entry with name: "
                    + dto.getName() + ") is not found.");
        }

        ValidateUtil.checkMarkedForDeletion(da, da.getName());

        if (!dto.getMcId().equals(da.getApplianceManagerConnector().getId())) {

            throw new VmidcBrokerValidationException("Appliance Manager Connector change is not allowed.");
        }

        validate(dto, false, da);

        return da;
    }

    // TODO: Emanoel - Add a unit test for image url update for DA
    void validate(DistributedApplianceDto dto) throws Exception {
        validate(dto, true, null);
    }

    void validate(DistributedApplianceDto dto, boolean forCreate, DistributedAppliance da) throws Exception {
        DistributedApplianceDtoValidator.checkForNullFields(dto);

        // Ensure DA name complies with Manager naming requirements
        if (!ValidateUtil.validateDaName(dto.getName())) {
            throw new VmidcBrokerValidationException(
                    "Invalid Distributed Appliance Name: "
                            + dto.getName()
                            + "DA name must not exceed 13 characters, must start with a letter, and can only contain numbers, letters and dash(-).");
        }

        DistributedApplianceDtoValidator.checkFieldLength(dto);

        Set<VirtualSystemDto> vsDtoList = dto.getVirtualizationSystems();

        if (vsDtoList == null || vsDtoList.size() == 0) {
            throw new VmidcBrokerValidationException(
                    "The associated Virtualization System must be selected for this Distributed Appliance.");
        }

        Appliance a = ApplianceEntityMgr.findById(this.em, dto.getApplianceId());

        if (a == null) {
            throw new VmidcBrokerValidationException(
                    "The associated Appliance must be selected for this Distributed Appliance.");
        }

        OSCEntityManager<ApplianceManagerConnector> mcMgr = new OSCEntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, this.em, this.txBroadcastUtil);
        ApplianceManagerConnector mc = mcMgr.findByPrimaryKey(dto.getMcId());

        if (mc == null) {
            throw new VmidcBrokerValidationException(
                    "The associated Appliance Manager Connector must be selected for this Distributed Appliance.");
        }

        List<Long> providedVsIds = new ArrayList<>();
        for (VirtualSystemDto vsDto : dto.getVirtualizationSystems()) {
            VirtualSystemDtoValidator.checkForNullFields(vsDto);

            VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(this.em, vsDto.getVcId());

            if (vc == null) {
                throw new VmidcBrokerValidationException(
                        "Distributed Appliance using the associated Virtualization Connector with Id: "
                                + vsDto.getVcId() + " does not exist.");
            }

            HashMap<String, Object> nullFields = new HashMap<>();
            HashMap<String, Object> notNullFields = new HashMap<>();

            boolean isPolicyMappingSupported = this.apiFactoryService.syncsPolicyMapping(mc.getManagerType());

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

            ApplianceSoftwareVersion av = ApplianceSoftwareVersionEntityMgr.findByApplianceVersionVirtTypeAndVersion(this.em,
                    dto.getApplianceId(), dto.getApplianceSoftwareVersionName(), vc.getVirtualizationType(),
                    vc.getVirtualizationSoftwareVersion());

            if (av == null) {
                throw new VmidcBrokerValidationException(
                        "Incompatible Distributed Appliance and The associated Appliance Software Version.");
            }

            if (forCreate && VirtualSystemEntityMgr.findByDAAndVC(this.em, dto.getId(), vsDto.getVcId()) != null) {
                throw new VmidcBrokerValidationException(
                        "The composite key Distributed Appliance, Virtualization Connector already exists.");
            }

            if (isPolicyMappingSupported) {
                OSCEntityManager<Domain> em = new OSCEntityManager<Domain>(Domain.class, this.em, this.txBroadcastUtil);
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

            providedVsIds.add(vsDto.getId());
        }

        if (!forCreate) {
            for (VirtualSystem vs : da.getVirtualSystems()) {
                // When updating a DA if the user is attempting to remove a VS that is being used to protect a workload fail the call.
                if (!providedVsIds.contains(vs.getId()) && VirtualSystemEntityMgr.isProtectingWorkload(vs)) {
                    throw new VmidcBrokerInvalidEntryException(String.format("The virtual system '%s' cannot be deleted. It is currently assigned to protect a workload.", vs.getName()));
                }
            }
        }
    }

    public static void checkForNullFields(DistributedApplianceDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Appliance Definition - Software Version", dto.getApplianceSoftwareVersionName());
        map.put("Secret Key", dto.getSecretKey());
        map.put("Distributed Appliance Name", dto.getName());

        ValidateUtil.checkForNullFields(map);
    }

    public static void checkFieldLength(DistributedApplianceDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("Secret Key", dto.getSecretKey());

        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }
}
