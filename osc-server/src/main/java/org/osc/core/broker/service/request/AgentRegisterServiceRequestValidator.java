package org.osc.core.broker.service.request;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;

public class AgentRegisterServiceRequestValidator implements RequestValidator<AgentRegisterServiceRequest, DistributedApplianceInstance> {
    private Session session;
    private static final Logger log = Logger.getLogger(AgentRegisterServiceRequestValidator.class);

    public AgentRegisterServiceRequestValidator(Session session) {
        this.session = session;
    }

    @Override
    public void validate(AgentRegisterServiceRequest request) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributedApplianceInstance validateAndLoad(AgentRegisterServiceRequest request) throws Exception {
        EntityManager<DistributedApplianceInstance> emgr = new EntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, this.session);

        DistributedApplianceInstance dai = null;

        if (request.getApplianceIp() == null) {
            throw new VmidcBrokerValidationException("Missing agent IP address.");
        }

        if (request.getVsId() == null) {
            throw new VmidcBrokerValidationException("Invalid virtual system identifier.");
        }

        // retrieve existing entry from db search by name
        if (request.getName() != null) {
            dai = emgr.findByFieldName("name", request.getName());
            if (dai == null) {
                log.warn("DAI '" + request.getName() + "' is registered but cannot be found in the database.");
            }
        }

        // if not found by name, search by ip
        if (dai == null && request.getApplianceIp() != null) {
            dai = emgr.findByFieldName("ipAddress", request.getApplianceIp());
            if (dai != null) {
                log.warn("DAI found by IP '" + request.getApplianceIp() + "'");
            }
        }

        VirtualSystem vs = null;
        if (dai != null) {
            vs = dai.getVirtualSystem();
        } else {
            EntityManager<VirtualSystem> vsMgr = new EntityManager<VirtualSystem>(VirtualSystem.class, this.session);
            vs = vsMgr.findByPrimaryKey(request.getVsId());
        }

        if (vs == null) {
            throw new VmidcBrokerValidationException("VS ID " + request.getVsId() + " not found.");
        }

        return dai;
    }
}
