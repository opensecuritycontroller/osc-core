package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;
import org.osc.core.broker.service.dto.openstack.HostDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.HostAggregateEntityMgr;
import org.osc.core.broker.service.persistence.HostEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;

public abstract class BaseDeploymentSpecService<I extends Request, O extends Response> extends ServiceDispatcher<I, O> {

    protected VirtualSystem vs;

    protected void validate(Session session, DeploymentSpecDto dto) throws Exception {
        DeploymentSpecDto.checkForNullFields(dto);
        DeploymentSpecDto.checkFieldLength(dto);

        this.vs = VirtualSystemEntityMgr.findById(session, dto.getParentId());

        if (this.vs == null || this.vs.getMarkedForDeletion()) {
            throw new VmidcBrokerValidationException(
                    "Deployment Specification using The associated Virtual System with Id: " + dto.getParentId()
                            + "  is either not found or is been marked deleted by the user.");
        }

        if (dto.getCount() == null || dto.getCount() <= 0) {
            throw new VmidcBrokerValidationException("Invalid count " + dto.getCount() == null ? "null"
                    : dto.getCount() + " specified for Deployment Specification");
        }

        if (!dto.getAvailabilityZones().isEmpty() && (!dto.getHosts().isEmpty() || !dto.getHostAggregates().isEmpty())
                || !dto.getHosts().isEmpty()
                && (!dto.getAvailabilityZones().isEmpty() || !dto.getHostAggregates().isEmpty())
                || !dto.getHostAggregates().isEmpty()
                && (!dto.getHosts().isEmpty() || !dto.getAvailabilityZones().isEmpty())) {
            // If multiple units of deployment are specified, throw an error
            throw new VmidcBrokerValidationException(
                    "Deployment Specification can only be specified with Availablity zones or Hosts or Host Aggregates.");
        }
        if (dto.getHosts().isEmpty() && dto.getCount() != 1) {
            throw new VmidcBrokerValidationException("Invalid count " + dto.getCount()
                    + " specified for Deployment Specification. Only valid value is 1.");
        }
    }

    protected void throwInvalidUpdateActionException(String attributeType, String dsName)
            throws VmidcBrokerValidationException {
        throw new VmidcBrokerValidationException(String.format(
                "'%s' attribute cannot be updated for Deployment Spec '%s'", attributeType, dsName));
    }

    protected HostAggregate createHostAggregate(Session session, HostAggregateDto haDto, DeploymentSpec ds) {
        HostAggregate ha = new HostAggregate(ds, haDto.getOpenstackId());
        HostAggregateEntityMgr.toEntity(ha, haDto);
        return EntityManager.create(session, ha);
    }

    protected AvailabilityZone createAvailabilityZone(Session session, AvailabilityZoneDto azDto, DeploymentSpec ds) {
        AvailabilityZone az = new AvailabilityZone(ds, azDto.getRegion(), azDto.getZone());
        return EntityManager.create(session, az);
    }

    protected Host createHost(Session session, HostDto hostDto, DeploymentSpec ds) {
        Host hs = new Host(ds, hostDto.getOpenstackId());
        HostEntityMgr.toEntity(hs, hostDto);
        return EntityManager.create(session, hs);
    }

}
