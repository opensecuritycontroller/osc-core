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
package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.TagEncapsulationType;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.api.AddDistributedApplianceServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.AddDistributedApplianceResponse;
import org.osc.core.broker.service.validator.DistributedApplianceDtoValidator;
import org.osc.core.broker.service.validator.DtoValidator;
import org.osc.core.broker.util.crypto.PKIUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class AddDistributedApplianceService
        extends ServiceDispatcher<BaseRequest<DistributedApplianceDto>, AddDistributedApplianceResponse>
        implements AddDistributedApplianceServiceApi {

    DtoValidator<DistributedApplianceDto, DistributedAppliance> validator;

    @Reference
    private DistributedApplianceDtoValidator validatorFactory;

    @Reference
    private DistributedApplianceConformJobFactory daConformJobFactory;

    @Reference
    private EncryptionApi encrypter;

    @Override
    public AddDistributedApplianceResponse exec(BaseRequest<DistributedApplianceDto> request, EntityManager em)
            throws Exception {

        OSCEntityManager<ApplianceManagerConnector> mcMgr = new OSCEntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, em, this.txBroadcastUtil);

        DistributedApplianceDto daDto = request.getDto();

        if (this.validator == null) {
            this.validator = this.validatorFactory.create(em);
        }

        this.validator.validateForCreate(daDto);

        ApplianceManagerConnector mc = mcMgr.findByPrimaryKey(daDto.getMcId());
        Appliance a = ApplianceEntityMgr.findById(em, daDto.getApplianceId());

        DistributedAppliance da = new DistributedAppliance(mc);

        List<VirtualSystem> vsList = getVirtualSystems(em, request.getDto(), da);

        // creating new entry in the db using entity manager object
        DistributedApplianceEntityMgr.createEntity(em, request.getDto(), a, da, this.encrypter);
        OSCEntityManager.create(em, da, this.txBroadcastUtil);

        for (VirtualSystem vs : vsList) {
            OSCEntityManager.create(em, vs, this.txBroadcastUtil);
            // Ensure name field is populated is assigned after DB id had been allocated
            vs.getName();
            da.addVirtualSystem(vs);
        }

        AddDistributedApplianceResponse response = new AddDistributedApplianceResponse();
        DistributedApplianceEntityMgr.fromEntity(da, response, this.encrypter);
        if(request.isApi()) {
            response.setSecretKey(null);
        }

        chain(() -> {

            Long jobId = startConformDAJob(da, em);

            response.setJobId(jobId);

            return response;
        });
        return null;
    }

    private Long startConformDAJob(DistributedAppliance da, EntityManager em) throws Exception {
        return this.daConformJobFactory.startDAConformJob(em, da);
    }

    List<VirtualSystem> getVirtualSystems(EntityManager em, DistributedApplianceDto daDto, DistributedAppliance da) throws Exception {
        List<VirtualSystem> vsList = new ArrayList<VirtualSystem>();

        // build the list of associated VirtualSystems for this DA
        Set<VirtualSystemDto> vsDtoList = daDto.getVirtualizationSystems();

        for (VirtualSystemDto vsDto : vsDtoList) {
            VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(em, vsDto.getVcId());

            // load the corresponding app sw version from db
            ApplianceSoftwareVersion av = ApplianceSoftwareVersionEntityMgr.findByApplianceVersionVirtTypeAndVersion(em,
                    daDto.getApplianceId(), daDto.getApplianceSoftwareVersionName(), vc.getVirtualizationType(),
                    vc.getVirtualizationSoftwareVersion());

            OSCEntityManager<Domain> oscEm = new OSCEntityManager<Domain>(Domain.class, em, this.txBroadcastUtil);
            Domain domain = vsDto.getDomainId() == null ? null : oscEm.findByPrimaryKey(vsDto.getDomainId());

            VirtualSystem vs = new VirtualSystem(da);

            vs.setApplianceSoftwareVersion(av);
            vs.setDomain(domain);
            vs.setVirtualizationConnector(vc);
            org.osc.sdk.controller.TagEncapsulationType encapsulationType = vsDto.getEncapsulationType();
            if(encapsulationType != null) {
                vs.setEncapsulationType(TagEncapsulationType.valueOf(
                    encapsulationType.name()));
            }
            // generate key store and persist it as byte array in db
            vs.setKeyStore(PKIUtil.generateKeyStore());
            vsList.add(vs);
        }

        return vsList;
    }
}
