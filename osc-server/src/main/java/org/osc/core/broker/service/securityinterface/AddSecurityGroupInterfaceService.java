package org.osc.core.broker.service.securityinterface;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;

public class AddSecurityGroupInterfaceService extends
BaseSecurityGroupInterfaceService<BaseRequest<SecurityGroupInterfaceDto>, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(AddSecurityGroupInterfaceService.class);

    @Override
    public BaseJobResponse exec(BaseRequest<SecurityGroupInterfaceDto> request, Session session) throws Exception {
        SecurityGroupInterfaceDto dto = request.getDto();
        validateAndLoad(session, dto);

        SecurityGroupInterface sgi = new SecurityGroupInterface(
                this.vs,
                null,
                null,
                dto.getFailurePolicyType(),
                0L);

        SecurityGroupInterfaceEntityMgr.toEntity(sgi, dto, this.policy, SecurityGroupInterface.ISC_TAG_PREFIX);

        log.info("Creating SecurityGroupInterface: " + sgi.toString());
        sgi = EntityManager.create(session, sgi);

        commitChanges(true);

        Long jobId = ConformService.startDAConformJob(session, sgi.getVirtualSystem().getDistributedAppliance());

        BaseJobResponse response = new BaseJobResponse(sgi.getId());
        response.setJobId(jobId);
        return response;
    }

    @Override
    protected void validateAndLoad(Session session, SecurityGroupInterfaceDto dto) throws Exception {
        super.validateAndLoad(session, dto);

        if (!dto.isUserConfigurable()) {
            throw new VmidcBrokerValidationException(
                    "Invalid request. Only User configured Traffic Policy Mappings can be Created.");
        }

        SecurityGroupInterface existingSGI = SecurityGroupInterfaceEntityMgr.findSecurityGroupInterfaceByVsAndTag(
                session, this.vs, SecurityGroupInterface.ISC_TAG_PREFIX + dto.getTagValue().toString());

        if (existingSGI != null) {
            throw new VmidcBrokerValidationException("A Traffic Policy Mapping: " + existingSGI.getName()
            + " exists for the specified virtual system and tag combination.");
        }

    }

}
