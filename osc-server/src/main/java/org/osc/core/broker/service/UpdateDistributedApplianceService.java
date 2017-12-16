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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.TagEncapsulationType;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.UpdateDistributedApplianceServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.validator.DistributedApplianceDtoValidator;
import org.osc.core.broker.service.validator.DtoValidator;
import org.osc.core.broker.util.crypto.PKIUtil;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UpdateDistributedApplianceService
        extends ServiceDispatcher<BaseRequest<DistributedApplianceDto>, BaseJobResponse>
        implements UpdateDistributedApplianceServiceApi {

    private static final Logger log = LoggerFactory.getLogger(UpdateDistributedApplianceService.class);

    private UnlockObjectMetaTask ult = null;

    private DtoValidator<DistributedApplianceDto, DistributedAppliance> validator;

    @Reference
    private DistributedApplianceConformJobFactory daConformJobFactory;

    @Reference
    private EncryptionApi encrypter;

    @Reference
    ApiFactoryService apiFactoryService;

    @Reference
    private DistributedApplianceDtoValidator validatorFactory;


    @Override
    public BaseJobResponse exec(BaseRequest<DistributedApplianceDto> request, EntityManager em)
            throws Exception {
        DistributedApplianceDto daDto = request.getDto();
        if (this.validator == null) {
            this.validator = this.validatorFactory.create(em);
        }

        try {
            DistributedAppliance da = this.validator.validateForUpdate(daDto);
            this.ult = LockUtil.tryLockDA(da, da.getApplianceManagerConnector());

            // reload the associated appliance
            Appliance a = ApplianceEntityMgr.findById(em, daDto.getApplianceId());

            updateVirtualSystems(em, daDto, da);

            // Broadcast notifications to UI if appliance version has changed, so that Appliance Instances view is
            // refreshed to reflect the correct version
            if (!daDto.getApplianceSoftwareVersionName().equals(da.getApplianceVersion())) {

                List<Long> daiIds = DistributedApplianceInstanceEntityMgr.listByDaId(em, da.getId());
                if (daiIds != null) {
                    for (Long daiId : daiIds) {
                        this.txBroadcastUtil.addMessageToMap(daiId,
                                DistributedApplianceInstance.class.getSimpleName(), EventType.UPDATED);
                    }
                }
            }

            DistributedApplianceEntityMgr.toEntity(a, da, daDto, this.encrypter);
            OSCEntityManager.update(em, da, this.txBroadcastUtil);

            UnlockObjectMetaTask forLambda = this.ult;
            chain(() -> {
                try {
                    Long jobId = startConformDAJob(da, em);
                    BaseJobResponse response = new BaseJobResponse();
                    response.setJobId(jobId);

                    return response;
                } catch (Exception e) {
                    LockUtil.releaseLocks(forLambda);
                    throw e;
                }
            });


        } catch (Exception e) {
            LockUtil.releaseLocks(this.ult);
            throw e;
        }

        return null;
    }

    private Long startConformDAJob(DistributedAppliance da, EntityManager em) throws Exception {
        return this.daConformJobFactory.startDAConformJob(em, da, this.ult);
    }

    /**
     * This function will figure out which VirtualSystem within the provided distributed appliance is new,
     * removed or unchanged and update them accordingly.
     * @param em
     *              The database session.
     * @param daDto
     *              The distributed appliance dto.
     * @param da
     *              The distributed appliance.
     * @throws Exception
     *              When any of the database operation fails.
     */
    private void updateVirtualSystems(EntityManager em, DistributedApplianceDto daDto, DistributedAppliance da) throws Exception {
        // get the request list
        Set<VirtualSystemDto> reqVs = daDto.getVirtualizationSystems();

        Set<Long> existingVsList = new HashSet<Long>();

        for (VirtualSystemDto vsDto : reqVs) {
            if (vsDto.getId() != null) {
                existingVsList.add(vsDto.getId());
            }
        }

        OSCEntityManager<VirtualSystem> vsEntityManager = new OSCEntityManager<VirtualSystem>(VirtualSystem.class, em, this.txBroadcastUtil);
        OSCEntityManager<Domain> domainEntityManager = new OSCEntityManager<Domain>(Domain.class, em, this.txBroadcastUtil);

        for (VirtualSystem vs : da.getVirtualSystems()) {
            if (!existingVsList.contains(vs.getId())) {
                vsEntityManager.markDeleted(vs);
            }
        }

        for (VirtualSystemDto vsDto : reqVs) {
            VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(em, vsDto.getVcId());

            // load the corresponding app sw version from db
            ApplianceSoftwareVersion av = ApplianceSoftwareVersionEntityMgr.findByApplianceVersionVirtTypeAndVersion(em, daDto
                    .getApplianceId(), daDto.getApplianceSoftwareVersionName(), vc.getVirtualizationType(), vc
                    .getVirtualizationSoftwareVersion());

            if (vsDto.getId() == null) { // new item

                // load domain
                Domain domain = vsDto.getDomainId() == null ? null
                        : domainEntityManager.findByPrimaryKey(vsDto.getDomainId());

                VirtualSystem newVs = new VirtualSystem(da);

                if (vsDto.getEncapsulationType() != null) {
                    newVs.setEncapsulationType(TagEncapsulationType.valueOf(vsDto.getEncapsulationType().name()));
                }
                newVs.setApplianceSoftwareVersion(av);
                newVs.setDomain(domain);
                newVs.setVirtualizationConnector(vc);

                // generate key store and persist it as byte array in db
                newVs.setKeyStore(PKIUtil.generateKeyStore());

                vsEntityManager.create(newVs);
                da.addVirtualSystem(newVs);
            } else {
                VirtualSystem existingVs = vsEntityManager.findByPrimaryKey(vsDto.getId());

                // TODO: Future. Handle updates for encapsulation type for openstack.
                // It is possible that the new version no longer support current encapsulation type,
                // or user may want to change it.
                existingVs.setApplianceSoftwareVersion(av);
                OSCEntityManager.update(em, existingVs, this.txBroadcastUtil);
            }
        }

        int activeVsCount = 0;
        VirtualSystem oneVs = null;
        for (VirtualSystem vs : da.getVirtualSystems()) {
            oneVs = vs;
            if (!vs.getMarkedForDeletion()) {
                activeVsCount++;
            }
        }

        if (activeVsCount == 0) {
            throw new VmidcBrokerValidationException(
                    "Distributed Appliance must have at least one active Virtual System.");
        }

        // If the DA version and the requested DA version dont match, assume its
        // an upgrade
        if (!da.getApplianceVersion().equals(daDto.getApplianceSoftwareVersionName())) {
            log.info("Upgrade/Downgrade of DA requested. Checking with Manager.");

            ManagerDeviceApi mgrApi = this.apiFactoryService.createManagerDeviceApi(oneVs);
            try {
                mgrApi.isUpgradeSupported(da.getAppliance().getModel(), da.getApplianceVersion(), daDto.getApplianceSoftwareVersionName());
            } finally {
                mgrApi.close();
            }

            log.info("Manager reported changing version from: " + da.getApplianceVersion() + " To: " + daDto.getApplianceSoftwareVersionName() + " is Supported.");

            /*
             * If we're upgrading, we'll need to reset all appliance configs
             * For SMC, initial config needs to be recreated.
             */
            for (VirtualSystem vs : da.getVirtualSystems()) {
                for (DistributedApplianceInstance dai : vs.getDistributedApplianceInstances()) {
                    dai.setApplianceConfig(null);
                    OSCEntityManager.update(em, dai, this.txBroadcastUtil);
                }
            }
        }
    }

}
