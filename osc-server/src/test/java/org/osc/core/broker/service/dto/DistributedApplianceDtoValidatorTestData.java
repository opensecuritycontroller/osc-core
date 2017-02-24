package org.osc.core.broker.service.dto;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.sdk.controller.TagEncapsulationType;

public class DistributedApplianceDtoValidatorTestData {
    static String EMPTY_VALUE_ERROR_MESSAGE = "should not have an empty value.";
    static String INVALID_FIELD_LENGTH_ERROR_MESSAGE = "length should not exceed 155 characters.";
    static String VALUE_IS_SET_ERROR_MESSAGE = "should not have a value set.";
    static Long APPLIANCE_ID_NOT_FOUND = 100L;
    static Long APPLIANCE_ID_EXISTING = 113L;
    static Long MC_ID_NOT_FOUND = 101L;
    static Long VC_ID_NOT_FOUND = 102L;
    static Long VC_ID_OPENSTACK = 103L;
    static Long VC_ID_VMWARE = 104L;
    static String VC_NAME_OPENSTACK = "VC_OPENSTACK";
    static Long DA_ID_EXISTING_VC = 105L;
    static String DA_NAME_EXISTING_DA = "EXISTINGDA";
    static String DA_NAME_NEW_DA = "NEWDA";
    static Long DA_ID_EXISTING_DA = 109L;
    static Long DA_ID_MISMATCHING_MC = 110L;
    static Long DOMAIN_ID_NOT_FOUND = 106L;
    static Long DOMAIN_ID_INVALID_NAME = 107L;
    static Long DOMAIN_ID_VALID_NAME = 108L;
    static Long MC_ID_VALID_MC = 111L;
    static Long MC_ID_POLICY_MAPPING_NOT_SUPPORTED_MC = 112L;
    static String SW_VERSION_NOT_FOUND = "SW_VERSION_NOT_FOUND";
    static String SW_VERSION_EXISTING_VC = "SW_VERSION_EXISTING_VC";

    static DistributedApplianceDto mcNotFoundDto = createDistributedApplianceDto();
    static DistributedApplianceDto vcNotFoundDto = createDistributedApplianceDto();
    static DistributedApplianceDto applianceSwVersionNotFoundDto = createDistributedApplianceDto();
    static DistributedApplianceDto daVcAlreadyExistsDto = createDistributedApplianceDto();
    static DistributedApplianceDto domainNotFoundDto = createDistributedApplianceDto();
    private static DistributedApplianceDto invalidDomainNameDto = createDistributedApplianceDto();

    static List<Object[]> getInvalidSwVersionTestData() {
        String[] invalidSwVersions = new String[]{null, ""};

        List<Object[]> result = new ArrayList<Object[]>();

        for(String invalidSwVersion : invalidSwVersions) {
            DistributedApplianceDto daDto = createDistributedApplianceDto();
            daDto.setApplianceSoftwareVersionName(invalidSwVersion);
            String errorMessage = "Appliance Definition - Software Version " + EMPTY_VALUE_ERROR_MESSAGE;

            result.add(new Object[] {daDto, VmidcBrokerInvalidEntryException.class, errorMessage});
        }

        return result;
    }

    static List<Object[]> getInvalidSecretKeyTestData() {
        String[] invalidSecretKeys = new String[]{null, "", StringUtils.rightPad("key", 156, 'e')};

        List<Object[]> result = new ArrayList<Object[]>();

        for(String invalidSecretKey : invalidSecretKeys) {
            DistributedApplianceDto daDto = createDistributedApplianceDto();
            daDto.setSecretKey(invalidSecretKey);

            String errorMessage = "Secret Key " +
                    (invalidSecretKey == null || invalidSecretKey.length() < 155 ?
                            EMPTY_VALUE_ERROR_MESSAGE : INVALID_FIELD_LENGTH_ERROR_MESSAGE);

            result.add(new Object[] {daDto,  VmidcBrokerInvalidEntryException.class, errorMessage});
        }

        return result;
    }

    static List<Object[]> getInvalidNameTestData() {
        String[] invalidNames = new String[]{null, "", "1ab", "#bc", "-ab", "ab12_", "ab a", StringUtils.rightPad("dtoName", 14, 'e')};

        List<Object[]> result = new ArrayList<Object[]>();

        for(String invalidName : invalidNames) {
            DistributedApplianceDto daDto = createDistributedApplianceDto();
            daDto.setName(invalidName);
            String errorMessage = invalidName == null || invalidName == "" ?
                    "Distributed Appliance Name " + EMPTY_VALUE_ERROR_MESSAGE :
                        "Invalid Distributed Appliance Name: "
                        + invalidName
                        + "DA name must not exceed 13 characters, must start with a letter, and can only contain numbers, letters and dash(-).";

            Class<?> expectedException = invalidName == null || invalidName == "" ?
                    VmidcBrokerInvalidEntryException.class : VmidcBrokerValidationException.class;

            result.add(new Object[] {daDto,  expectedException, errorMessage});
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    static List<Object[]> getInvalidVirtualizationSystemsCollectionData() {
        Set<?>[] invalidVirtualSystemCollections = new HashSet<?>[]{null, new HashSet<VirtualSystemDto>()};

        List<Object[]> result = new ArrayList<Object[]>();

        for(Set<?> invalidVirtualSystemCollection : invalidVirtualSystemCollections) {
            DistributedApplianceDto daDto = createDistributedApplianceDto();
            daDto.setVirtualizationSystems((Set<VirtualSystemDto>) invalidVirtualSystemCollection);

            result.add(new Object[] {daDto,  VmidcBrokerValidationException.class,
            "The associated Virtualization System must be selected for this Distributed Appliance."});
        }

        return result;
    }

    static Object[] getInvalidApplianceIdTestData() {
        DistributedApplianceDto applianceNotFoundDto = createDistributedApplianceDto();
        applianceNotFoundDto.setApplianceId(APPLIANCE_ID_NOT_FOUND);

        Object[] result = new Object[]{applianceNotFoundDto,
                VmidcBrokerValidationException.class,
        "The associated Appliance must be selected for this Distributed Appliance."};

        return result;
    }

    static Object[] getInvalidMngrConnectorIdTestData() {
        mcNotFoundDto.setMcId(MC_ID_NOT_FOUND);
        Object[] result = new Object[]{mcNotFoundDto,
                VmidcBrokerValidationException.class,
        "The associated Appliance Manager Connector must be selected for this Distributed Appliance."};

        return result;
    }

    static List<Object[]> getInvalidVcIdTestData() {
        List<Object[]> result = new ArrayList<Object[]>();

        DistributedApplianceDto daDto = createDistributedApplianceDto();
        ((VirtualSystemDto)daDto.getVirtualizationSystems().toArray()[0]).setVcId(null);

        result.add(new Object[] {daDto,  VmidcBrokerInvalidEntryException.class, "Virtualization Connector Id " + EMPTY_VALUE_ERROR_MESSAGE});

        ((VirtualSystemDto)vcNotFoundDto.getVirtualizationSystems().toArray()[0]).setVcId(VC_ID_NOT_FOUND);

        result.add(new Object[]{vcNotFoundDto, VmidcBrokerValidationException.class,
                MessageFormat.format("Distributed Appliance using the associated Virtualization "
                        + "Connector with Id: {0} does not exist.", VC_ID_NOT_FOUND)});
        return result;
    }

    static List<Object[]> getInvalidEncapsulationTypeTestData() {
        List<Object[]> result = new ArrayList<Object[]>();

        DistributedApplianceDto vmWareDaDto = createDistributedApplianceDto();
        DistributedApplianceDto openStackDaDto = createDistributedApplianceDto();
        DistributedApplianceDto openStackPolicyMappingNotSupportedDaDto = createDistributedApplianceDto();

        // VMWARE VS SHOULD NOT have encapsulation type set
        ((VirtualSystemDto)vmWareDaDto.getVirtualizationSystems().toArray()[0]).setVcId(VC_ID_VMWARE);
        ((VirtualSystemDto)vmWareDaDto.getVirtualizationSystems().toArray()[0]).setEncapsulationType(TagEncapsulationType.VLAN);

        // OPENSTACK VS SHOULD have encapsulation type set
        ((VirtualSystemDto)openStackDaDto.getVirtualizationSystems().toArray()[0]).setVcId(VC_ID_OPENSTACK);
        ((VirtualSystemDto)openStackDaDto.getVirtualizationSystems().toArray()[0]).setEncapsulationType(null);

        // OPENSTACK VS SHOULD NOT have encapsulation type set when manage does not support policy mapping
        ((VirtualSystemDto)openStackPolicyMappingNotSupportedDaDto.getVirtualizationSystems().toArray()[0]).setVcId(VC_ID_OPENSTACK);
        ((VirtualSystemDto)openStackPolicyMappingNotSupportedDaDto.getVirtualizationSystems().toArray()[0]).setEncapsulationType(TagEncapsulationType.VLAN);
        ((VirtualSystemDto)openStackPolicyMappingNotSupportedDaDto.getVirtualizationSystems().toArray()[0]).setDomainId(null);
        openStackPolicyMappingNotSupportedDaDto.setMcId(MC_ID_POLICY_MAPPING_NOT_SUPPORTED_MC);

        result.add(new Object[] {vmWareDaDto,  VmidcBrokerInvalidEntryException.class, "Encapsulation Type " + VALUE_IS_SET_ERROR_MESSAGE});
        result.add(new Object[] {openStackDaDto,  VmidcBrokerInvalidEntryException.class, "Encapsulation Type " + EMPTY_VALUE_ERROR_MESSAGE});
        result.add(new Object[] {openStackPolicyMappingNotSupportedDaDto,  VmidcBrokerInvalidEntryException.class, "Encapsulation Type " + VALUE_IS_SET_ERROR_MESSAGE});

        return result;
    }

    static Object[] getInvalidApplianceSoftwareVersionTestData() {
        ((VirtualSystemDto)applianceSwVersionNotFoundDto.getVirtualizationSystems().toArray()[0]).setVcId(VC_ID_VMWARE);
        ((VirtualSystemDto)applianceSwVersionNotFoundDto.getVirtualizationSystems().toArray()[0]).setEncapsulationType(null);

        applianceSwVersionNotFoundDto.setApplianceSoftwareVersionName(SW_VERSION_NOT_FOUND);

        Object[] result =  new Object[] {applianceSwVersionNotFoundDto,  VmidcBrokerValidationException.class, "Incompatible Distributed Appliance and The associated Appliance Software Version."};

        return result;
    }

    static Object[] getDaVcAlreadyExistsTestData() {
        daVcAlreadyExistsDto.setId(DA_ID_EXISTING_VC);

        Object[] result =  new Object[] {daVcAlreadyExistsDto,  VmidcBrokerValidationException.class, "The composite key Distributed Appliance, Virtualization Connector already exists."};

        return result;
    }

    static List<Object[]> getInvalidDomainTestData() {
        List<Object[]> result = new ArrayList<Object[]>();
        DistributedApplianceDto nullDomainIdDto = createDistributedApplianceDto();
        DistributedApplianceDto mcPolicyMappingNotSupported = createDistributedApplianceDto();
        mcPolicyMappingNotSupported.setMcId(MC_ID_POLICY_MAPPING_NOT_SUPPORTED_MC);

        setVirtualSystemDomain(domainNotFoundDto, DOMAIN_ID_NOT_FOUND);
        setVirtualSystemDomain(invalidDomainNameDto, DOMAIN_ID_INVALID_NAME);
        setVirtualSystemDomain(nullDomainIdDto, null);

        result.add(new Object[] {domainNotFoundDto,  VmidcBrokerValidationException.class, MessageFormat.format("Domain used in Virtual System {0} cannot be found.", VC_NAME_OPENSTACK)});
        result.add(new Object[] {invalidDomainNameDto,  VmidcBrokerInvalidEntryException.class, MessageFormat.format("Invalid domain length found in Virtual System {0}.", VC_NAME_OPENSTACK)});
        result.add(new Object[] {nullDomainIdDto,  VmidcBrokerInvalidEntryException.class, "Domain Id " + EMPTY_VALUE_ERROR_MESSAGE});
        result.add(new Object[] {mcPolicyMappingNotSupported,  VmidcBrokerInvalidEntryException.class, "Domain Id " + VALUE_IS_SET_ERROR_MESSAGE});

        return result;
    }

    private static void setVirtualSystemDomain(DistributedApplianceDto daDto, Long domainId) {
        ((VirtualSystemDto)daDto.getVirtualizationSystems().toArray()[0]).setDomainId(domainId);
    }

    static DistributedApplianceDto createDistributedApplianceDto() {
        DistributedApplianceDto daDto = new DistributedApplianceDto();
        daDto.setSecretKey("secretKey");
        daDto.setName("daName");
        daDto.setApplianceId(APPLIANCE_ID_EXISTING);
        daDto.setMcId(MC_ID_VALID_MC);
        daDto.setApplianceSoftwareVersionName(SW_VERSION_EXISTING_VC);
        Set<VirtualSystemDto> virtualSystems = new HashSet<VirtualSystemDto>();
        VirtualSystemDto virtualSystem = new VirtualSystemDto();
        virtualSystem.setDomainId(DOMAIN_ID_VALID_NAME);
        virtualSystem.setVcId(VC_ID_OPENSTACK);
        virtualSystem.setEncapsulationType(TagEncapsulationType.VLAN);

        virtualSystems.add(virtualSystem);

        daDto.setVirtualizationSystems(virtualSystems);

        return daDto;
    }
}
