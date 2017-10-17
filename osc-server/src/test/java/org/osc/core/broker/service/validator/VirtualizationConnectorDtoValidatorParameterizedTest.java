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

import static org.osc.core.broker.service.validator.DistributedApplianceDtoValidatorTestData.*;
import static org.osc.core.common.virtualization.VirtualizationConnectorProperties.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.server.Server;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
public class VirtualizationConnectorDtoValidatorParameterizedTest extends VirtualizationConnectorDtoValidatorBaseTest {
    private VirtualizationConnectorDto dtoParam;
    private Class<Throwable> exceptionTypeParam;
    private String expectedErrorMessageParam;

    public VirtualizationConnectorDtoValidatorParameterizedTest(VirtualizationConnectorDto dto,
            Class<Throwable> exceptionType, String expectedErrorMessage) {
        this.dtoParam = dto;
        this.exceptionTypeParam = exceptionType;
        this.expectedErrorMessageParam = expectedErrorMessage;
    }

    @Test
    public void testValidateForCreate_UsingInvalidField_ThrowsExpectedException() throws Exception {
        // Arrange.
        this.exception.expect(this.exceptionTypeParam);
        this.exception.expectMessage(this.expectedErrorMessageParam);

        // Act.
        this.dtoValidator.validateForCreate(this.dtoParam);
    }

    @Parameters()
    public static Collection<Object[]> getInvalidFieldsTestData() throws Exception {
        List<Object[]> result = new ArrayList<Object[]>();

        result.addAll(getInvalidNameTestData());
        result.addAll(getInvalidControllerUserNameTestData());
        result.addAll(getInvalidProviderUserNameTestData());
        result.addAll(getInvalidControllerPasswordTestData());
        result.addAll(getInvalidProviderPaswordTestData());
        result.addAll(getInvalidControllerIpTestData());
        result.addAll(getInvalidProviderIpTestData());
        result.addAll(getInvalidSoftwareVersions());
        result.addAll(getInvalidProjectNameTestData());
        result.addAll(getInvalidMqUserTestData());
        result.addAll(getInvalidMqPasswordTestData());
        result.addAll(getInvalidMqPortTestData());
        result.addAll(getInvalidIpAddresses());
        result.addAll(getInvalidControllerTypeTestData());
        return result;
    }

    static List<Object[]> getInvalidIpAddresses() {
        String[] invalidIps = new String[] { "abc", "2.a.1.5", "2a.1", "8.8.8.8888", "127.0.0..1", "127.0.0.1.3" };

        List<Object[]> result = new ArrayList<>();

        for (String invalidIp : invalidIps) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithoutSDN();
            vcDto.getProviderAttributes().put(ATTRIBUTE_KEY_RABBITMQ_IP, invalidIp);
            String errorMessage = Server.PRODUCT_NAME + ": " + "IP Address: '" + invalidIp + "' has invalid format.";

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidMqPortTestData() {
        String[] invalidNames = new String[] { null, "",
                StringUtils.rightPad("dtoName", ValidateUtil.DEFAULT_MAX_LEN + 10, 'e') };

        List<Object[]> result = new ArrayList<>();

        for (String invalidName : invalidNames) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithoutSDN();
            vcDto.getProviderAttributes().put(ATTRIBUTE_KEY_RABBITMQ_PORT, invalidName);

            String errorMessage;
            if (invalidName == null || invalidName.equals("")) {
                errorMessage = Server.PRODUCT_NAME + ": " + "Rabbit MQ Port " + EMPTY_VALUE_ERROR_MESSAGE;
            } else if (!StringUtils.isNumeric(invalidName)) {
                errorMessage = ATTRIBUTE_KEY_RABBITMQ_PORT
                        + " expected to be an Integer. Value is: " + invalidName;
            } else {
                errorMessage = Server.PRODUCT_NAME + ": " + ATTRIBUTE_KEY_RABBITMQ_PORT
                        + " length should not exceed " + ValidateUtil.DEFAULT_MAX_LEN
                        + " characters. The provided field exceeds this limit by "
                        + (invalidName.length() - ValidateUtil.DEFAULT_MAX_LEN) + " characters.";
            }

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidMqPasswordTestData() {
        String[] invalidNames = new String[] { null, "" };

        List<Object[]> result = new ArrayList<>();

        for (String invalidName : invalidNames) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithoutSDN();
            vcDto.getProviderAttributes().put(ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD,
                    invalidName);
            String errorMessage = invalidName == null || invalidName.equals("")
                    ? Server.PRODUCT_NAME + ": " + "Rabbit MQ Password " + EMPTY_VALUE_ERROR_MESSAGE
                            : Server.PRODUCT_NAME + ": " + ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD
                            + " length should not exceed " + ValidateUtil.DEFAULT_MAX_LEN
                            + " characters. The provided field exceeds this limit by "
                            + (invalidName.length() - ValidateUtil.DEFAULT_MAX_LEN) + " characters.";

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidMqUserTestData() {
        String[] invalidNames = new String[] { null, "" };

        List<Object[]> result = new ArrayList<>();

        for (String invalidName : invalidNames) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithoutSDN();
            vcDto.getProviderAttributes().put(ATTRIBUTE_KEY_RABBITMQ_USER, invalidName);
            String errorMessage = invalidName == null || invalidName.equals("")
                    ? Server.PRODUCT_NAME + ": " + "Rabbit MQ User " + EMPTY_VALUE_ERROR_MESSAGE
                            : Server.PRODUCT_NAME + ": " + ATTRIBUTE_KEY_RABBITMQ_USER
                            + " length should not exceed " + ValidateUtil.DEFAULT_MAX_LEN
                            + " characters. The provided field exceeds this limit by "
                            + (invalidName.length() - ValidateUtil.DEFAULT_MAX_LEN) + " characters.";

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidProjectNameTestData() {
        Map<VirtualizationType, String[]> invalidNames = new HashMap<>();
        invalidNames.put(VirtualizationType.OPENSTACK,
                new String[] {
                        null,
                        "",
                        StringUtils.rightPad("dtoName", ValidateUtil.DEFAULT_MAX_LEN + 10, 'e')
        });

        invalidNames.put(VirtualizationType.KUBERNETES,
                new String[] {
                        "NotNullName"
        });

        List<Object[]> result = new ArrayList<>();
        invalidNames.forEach((k, v) -> {
            for (String invalidName : v) {
                String errorMessage = null;
                VirtualizationConnectorDto vcDto = null;
                if (k.equals(VirtualizationType.OPENSTACK)) {
                    vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithoutSDN();
                    errorMessage = invalidName == null || invalidName.equals("")
                            ? Server.PRODUCT_NAME + ": " + "Admin Project Name " + EMPTY_VALUE_ERROR_MESSAGE
                                    : Server.PRODUCT_NAME + ": " + "Admin Project Name" + " length should not exceed "
                                    + ValidateUtil.DEFAULT_MAX_LEN + " characters. The provided field exceeds this limit by "
                                    + (invalidName.length() - ValidateUtil.DEFAULT_MAX_LEN) + " characters.";
                } else {
                    vcDto = VirtualizationConnectorDtoValidatorTestData.generateK8skVCWithSDN();
                    errorMessage = Server.PRODUCT_NAME + ": " + "Admin Project Name " + VALUE_IS_SET_ERROR_MESSAGE;
                }

                vcDto.setAdminProjectName(invalidName);

                Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

                result.add(new Object[] { vcDto, expectedException, errorMessage });
            }
        });

        return result;
    }

    static List<Object[]> getInvalidSoftwareVersions() {
        String[] invalidVersions = new String[] { null, "" };

        List<Object[]> result = new ArrayList<>();

        for (String invalidVersion : invalidVersions) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithoutSDN();
            vcDto.setSoftwareVersion(invalidVersion);
            String errorMessage = Server.PRODUCT_NAME + ": " + "Software Version " + EMPTY_VALUE_ERROR_MESSAGE;

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidProviderIpTestData() {
        String[] invalidNames = new String[] { null, "" };

        List<Object[]> result = new ArrayList<>();

        for (String invalidName : invalidNames) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithoutSDN();
            vcDto.setProviderIP(invalidName);
            String errorMessage = Server.PRODUCT_NAME + ": " + "Provider IP Address " + EMPTY_VALUE_ERROR_MESSAGE;

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidControllerIpTestData() {
        String[] invalidNames = new String[] { null, "" };

        List<Object[]> result = new ArrayList<>();

        for (String invalidName : invalidNames) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithSDN();
            vcDto.setControllerIP(invalidName);
            String errorMessage = Server.PRODUCT_NAME + ": " + "Controller IP Address " + EMPTY_VALUE_ERROR_MESSAGE;

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidControllerTypeTestData() {
        final String ERROR_MSG = "Virtualization connectors for Kubernetes must have a SDN controller.";
        List<Object[]> result = new ArrayList<>();
        result.add(new Object[] {VirtualizationConnectorDtoValidatorTestData.generateK8sVCWithoSDNNone(), VmidcBrokerValidationException.class, ERROR_MSG });

        return result;
    }

    static List<Object[]> getInvalidNameTestData() {
        String[] invalidNames = new String[] { null, "",
                StringUtils.rightPad("dtoName", ValidateUtil.DEFAULT_MAX_LEN + 10, 'e') };

        List<Object[]> result = new ArrayList<>();

        for (String invalidName : invalidNames) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithoutSDN();
            vcDto.setName(invalidName);
            String errorMessage = invalidName == null || invalidName == ""
                    ? Server.PRODUCT_NAME + ": " + "Name " + EMPTY_VALUE_ERROR_MESSAGE
                            : Server.PRODUCT_NAME + ": " + "Name" + " length should not exceed " + ValidateUtil.DEFAULT_MAX_LEN
                            + " characters. The provided field exceeds this limit by "
                            + (invalidName.length() - ValidateUtil.DEFAULT_MAX_LEN) + " characters.";

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidControllerPasswordTestData() {
        String[] invalidNames = new String[] { null, "",
                StringUtils.rightPad("dtoName", ValidateUtil.DEFAULT_MAX_LEN + 10, 'e') };

        List<Object[]> result = new ArrayList<Object[]>();

        for (String invalidName : invalidNames) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithSDN();
            vcDto.setControllerPassword(invalidName);
            String errorMessage = invalidName == null || invalidName == ""
                    ? Server.PRODUCT_NAME + ": " + "Controller Password " + EMPTY_VALUE_ERROR_MESSAGE
                            : Server.PRODUCT_NAME + ": " + "Controller Password" + " length should not exceed "
                            + ValidateUtil.DEFAULT_MAX_LEN + " characters. The provided field exceeds this limit by "
                            + (invalidName.length() - ValidateUtil.DEFAULT_MAX_LEN) + " characters.";

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidProviderPaswordTestData() {
        String[] invalidNames = new String[] { null, "",
                StringUtils.rightPad("dtoName", ValidateUtil.DEFAULT_MAX_LEN + 10, 'e') };

        List<Object[]> result = new ArrayList<Object[]>();

        for (String invalidName : invalidNames) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithSDN();
            vcDto.setProviderPassword(invalidName);
            String errorMessage = invalidName == null || invalidName == ""
                    ? Server.PRODUCT_NAME + ": " + "Provider Password " + EMPTY_VALUE_ERROR_MESSAGE
                            : Server.PRODUCT_NAME + ": " + "Provider Password" + " length should not exceed "
                            + ValidateUtil.DEFAULT_MAX_LEN + " characters. The provided field exceeds this limit by "
                            + (invalidName.length() - ValidateUtil.DEFAULT_MAX_LEN) + " characters.";

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidControllerUserNameTestData() throws Exception {
        String[] invalidNames = new String[] { null, "",
                StringUtils.rightPad("dtoName", ValidateUtil.DEFAULT_MAX_LEN + 10, 'e') };

        List<Object[]> result = new ArrayList<Object[]>();

        for (String invalidName : invalidNames) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithSDN();
            vcDto.setControllerUser(invalidName);
            String errorMessage = invalidName == null || invalidName == ""
                    ? Server.PRODUCT_NAME + ": " + "Controller User Name " + EMPTY_VALUE_ERROR_MESSAGE
                            : Server.PRODUCT_NAME + ": " + "Controller User Name" + " length should not exceed "
                            + ValidateUtil.DEFAULT_MAX_LEN + " characters. The provided field exceeds this limit by "
                            + (invalidName.length() - ValidateUtil.DEFAULT_MAX_LEN) + " characters.";

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }

    static List<Object[]> getInvalidProviderUserNameTestData() {
        String[] invalidNames = new String[] { null, "",
                StringUtils.rightPad("dtoName", ValidateUtil.DEFAULT_MAX_LEN + 10, 'e') };

        List<Object[]> result = new ArrayList<Object[]>();

        for (String invalidName : invalidNames) {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorDtoValidatorTestData.generateOpenStackVCWithSDN();
            vcDto.setProviderUser(invalidName);
            String errorMessage = invalidName == null || invalidName == ""
                    ? Server.PRODUCT_NAME + ": " + "Provider User Name " + EMPTY_VALUE_ERROR_MESSAGE
                            : Server.PRODUCT_NAME + ": " + "Provider User Name" + " length should not exceed "
                            + ValidateUtil.DEFAULT_MAX_LEN + " characters. The provided field exceeds this limit by "
                            + (invalidName.length() - ValidateUtil.DEFAULT_MAX_LEN) + " characters.";

            Class<?> expectedException = VmidcBrokerInvalidEntryException.class;

            result.add(new Object[] { vcDto, expectedException, errorMessage });
        }

        return result;
    }
}