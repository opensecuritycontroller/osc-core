package org.osc.core.broker.service.vc;

import java.util.HashMap;
import java.util.Map;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;

public class VirtualizationConnectorServiceData {

    public static String VMWARE_NAME_ALREADY_EXISTS = "VMWare Name";
    public static String OPENSTACK_NAME_ALREADY_EXISTS = "Openstack Name";
    public static String CONTROLLER_IP_ALREADY_EXISTS = "127.0.0.1";
    public static String PROVIDER_IP_ALREADY_EXISTS = "127.0.0.2";

    public static DryRunRequest<VirtualizationConnectorDto> VMWARE_NAME_ALREADY_EXISTS_REQUEST = createVmWareRequest(
            VirtualizationType.VMWARE, VMWARE_NAME_ALREADY_EXISTS, "1.1.1.1", "controller user", "controller password",
            "2.2.2.2", "provider user", "provider Password", "4.3");

    public static DryRunRequest<VirtualizationConnectorDto> CONTROLLER_IP_ALREADY_EXISTS_VMWARE_REQUEST = createVmWareRequest(
            VirtualizationType.VMWARE, "Random VMWare name", CONTROLLER_IP_ALREADY_EXISTS, "controller user",
            "controller password", "2.2.2.2", "provider user", "provider Password", "4.3");

    public static DryRunRequest<VirtualizationConnectorDto> PROVIDER_IP_ALREADY_EXISTS_VMWARE_REQUEST = createVmWareRequest(
            VirtualizationType.VMWARE, "Random VMWare name", "1.1.1.1", "controller user", "controller password",
            PROVIDER_IP_ALREADY_EXISTS, "provider user", "provider Password", "4.3");

    public static DryRunRequest<VirtualizationConnectorDto> VMWARE_REQUEST = createVmWareRequest(
            VirtualizationType.VMWARE, "Random VMWare name", "1.1.1.1", "controller user", "controller password",
            "2.2.2.2", "provider user", "provider Password", "4.3");

    public static DryRunRequest<VirtualizationConnectorDto> OPENSTACK_NAME_ALREADY_EXISTS_NOCONTROLLER_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, OPENSTACK_NAME_ALREADY_EXISTS, null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Tenant Demo", "RabbitMq User", "RabbitMq Password", "1111", null);

    public static DryRunRequest<VirtualizationConnectorDto> OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, OPENSTACK_NAME_ALREADY_EXISTS, null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Tenant Demo", "RabbitMq User", "RabbitMq Password", "1111", "NSC");

    public static DryRunRequest<VirtualizationConnectorDto> PROVIDER_IP_ALREADY_EXISTS_OPENSTACK_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, "Random Openstack name", null, null, null, PROVIDER_IP_ALREADY_EXISTS,
            "provider user", "provider Password", "4.3", "Tenant Demo", "RabbitMq User", "RabbitMq Password", "1111",
            null);

    public static DryRunRequest<VirtualizationConnectorDto> OPENSTACK_NSC_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, "Random Openstack name", null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Tenant Demo", "RabbitMq User", "RabbitMq Password", "1111", "NSC");

    public static DryRunRequest<VirtualizationConnectorDto> OPENSTACK_NOCONTROLLER_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, "Random Openstack name", null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Tenant Demo", "RabbitMq User", "RabbitMq Password", "1111", null);

    private static DryRunRequest<VirtualizationConnectorDto> createRequest(
            VirtualizationType virtualizationType,
            String name,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version) {

		DryRunRequest<VirtualizationConnectorDto> request = new DryRunRequest<>();
		VirtualizationConnectorDto dto = getVCDto(virtualizationType, name, controllerIp, controllerUser,
				controllerPassword, providerIp, providerUser, providerPassword, version);
		request.setDto(dto);
        
        return request;
    }
    
    private static VirtualizationConnectorDto getVCDto(VirtualizationType virtualizationType,
            String name,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version){
    	
    	VirtualizationConnectorDto dto = new VirtualizationConnectorDto();
    	dto.setType(virtualizationType);
        dto.setName(name);
        dto.setControllerIP(controllerIp);
        dto.setControllerUser(controllerUser);
        dto.setControllerPassword(controllerPassword);
        dto.setProviderIP(providerIp);
        dto.setProviderUser(providerUser);
        dto.setProviderPassword(providerPassword);
        dto.setSoftwareVersion(version);
        
        return dto;
    }

    public static VirtualizationConnectorDto getVCDtoforVmware(){
    	
    	return getVCDto(VirtualizationType.VMWARE, "Random VMWare name", "1.1.1.1", "controller user", "controller password",
                "2.2.2.2", "provider user", "provider Password", "4.3");
    }

    public static VirtualizationConnectorDto getVCDtoforOpenStack(){
    	
    	VirtualizationConnectorDto vcDto = getVCDto(VirtualizationType.OPENSTACK, "Random Openstack name", null, null, null, "2.2.2.2", "provider user",
                "provider Password", "4.3");
    	setOpenStackParams(vcDto, "Tenant Demo", "RabbitMq User", "RabbitMq Password", "1111", null);
    	
    	return vcDto;
    }
    
    public static VirtualizationConnectorDto getVCDtoforOpenStackwithSDN(){
    	
    	VirtualizationConnectorDto vcDto = getVCDto(VirtualizationType.OPENSTACK, "Random Openstack name", "1.1.1.1", "controller user", "controller password", "2.2.2.2", "provider user",
                "provider Password", "4.3");
    	setOpenStackParams(vcDto, "Tenant Demo", "RabbitMq User", "RabbitMq Password", "1111", "NSC");
    	
    	return vcDto;
    }
    
    private static void setOpenStackParams(VirtualizationConnectorDto vcDto,
    		String tenantName,
            String rabbitMq_user,
            String rabbitMq_password,
            String rabbitMq_port,
            String controllerTypeStr){
    	
    	vcDto.setAdminTenantName(tenantName);

         Map<String, String> providerAttributes = new HashMap<>();
         providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER, rabbitMq_user);
         providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD, rabbitMq_password);
         providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT, rabbitMq_port);
         vcDto.setProviderAttributes(providerAttributes);
         
         if (controllerTypeStr != null && (!controllerTypeStr.isEmpty())) {
             ControllerType.addType(controllerTypeStr);
             ControllerType controllerType = ControllerType.fromText(controllerTypeStr);
             vcDto.setControllerType(controllerType);
         }
    }
    
    private static DryRunRequest<VirtualizationConnectorDto> createOpenStackRequest(
            VirtualizationType virtualizationType,
            String name,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version,
            String tenantName,
            String rabbitMq_user,
            String rabbitMq_password,
            String rabbitMq_port,
            String controllerTypeStr) {

        DryRunRequest<VirtualizationConnectorDto> request = createRequest(virtualizationType, name, controllerIp,
                controllerUser, controllerPassword, providerIp, providerUser, providerPassword, version);
        VirtualizationConnectorDto dto = request.getDto();
        setOpenStackParams(dto, tenantName, rabbitMq_user, rabbitMq_password, rabbitMq_port, controllerTypeStr);
        
        return request;
    }

    private static DryRunRequest<VirtualizationConnectorDto> createVmWareRequest(
            VirtualizationType virtualizationType,
            String name,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version) {

        return createRequest(virtualizationType, name, controllerIp, controllerUser, controllerPassword, providerIp,
                providerUser, providerPassword, version);
    }

	public static DryRunRequest<VirtualizationConnectorDto> getVmwareRequest() {
		
		DryRunRequest<VirtualizationConnectorDto> request = new DryRunRequest<>();
		request.setDto(getVCDtoforVmware());
		
		return request;
	}
	
	public static DryRunRequest<VirtualizationConnectorDto> getOpenStackRequestwithSDN() {
		
		DryRunRequest<VirtualizationConnectorDto> request = new DryRunRequest<>();
		request.setDto(getVCDtoforOpenStackwithSDN());
		
		return request;
	}

}