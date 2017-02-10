package org.osc.core.broker.service.request;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.virtualization.OpenstackSoftwareVersion;
import org.osc.core.broker.model.virtualization.VmwareSoftwareVersion;
import org.osc.core.broker.service.dto.DtoValidator;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.util.ValidateUtil;

public class VirtualizationConnectorDtoValidator
		implements DtoValidator<VirtualizationConnectorDto, VirtualizationConnector> {

	private Session session;
	private static final Logger LOG = Logger.getLogger(VirtualizationConnectorDtoValidator.class);
	
	public VirtualizationConnectorDtoValidator(Session session) {
		this.session = session;
	}

	@Override
	public void validateForCreate(VirtualizationConnectorDto dto) throws Exception {
		// Initializing Entity Manager
        EntityManager<VirtualizationConnector> emgr = new EntityManager<>(
                VirtualizationConnector.class, this.session);
        
        VirtualizationConnectorDto.checkForNullFields(dto);
        VirtualizationConnectorDto.checkFieldLength(dto);

        // TODO: Future. Right now we assume Icehouse and 5.5 regardless of the actual version passed in, need to
        // fix this later.
        if (dto.getType().isOpenstack()) {
            dto.setSoftwareVersion(OpenstackSoftwareVersion.OS_ICEHOUSE.toString());
        } else if (dto.getType().isVmware()) {
            dto.setSoftwareVersion(VmwareSoftwareVersion.VMWARE_V5_5.toString());
        }

        // check for uniqueness of vc name
        if (emgr.isExisting("name", dto.getName())) {

            throw new VmidcBrokerValidationException(
                    "Virtualization Connector Name: " + dto.getName() + " already exists.");
        }

        // check for valid IP address format
        if (!dto.getType().isOpenstack()) {
            ValidateUtil.checkForValidIpAddressFormat(dto.getControllerIP());

            // check for uniqueness of vc nsx IP
            if (emgr.isExisting("controllerIpAddress", dto.getControllerIP())) {

                throw new VmidcBrokerValidationException(
                        "Controller IP Address: " + dto.getControllerIP() + " already exists.");
            }
        }

        VirtualizationConnectorDto.checkFieldFormat(dto);

        // check for uniqueness of vc vCenter IP
        if (emgr.isExisting("providerIpAddress", dto.getProviderIP())) {

            throw new VmidcBrokerValidationException(
                    "Provider IP Address: " + dto.getProviderIP() + " already exists.");
        }


	}

	@Override
	public VirtualizationConnector validateForUpdate(VirtualizationConnectorDto dto) throws Exception {
		throw new UnsupportedOperationException();
	}

}
