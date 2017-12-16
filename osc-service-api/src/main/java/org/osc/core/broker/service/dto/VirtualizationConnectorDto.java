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
package org.osc.core.broker.service.dto;

import static org.osc.core.common.virtualization.VirtualizationConnectorProperties.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD;
import static org.osc.core.common.virtualization.VirtualizationConnectorProperties.NO_CONTROLLER_TYPE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.common.virtualization.VirtualizationType;

import io.swagger.annotations.ApiModelProperty;

// Virtualization Connector Data Transfer Object associated with VC entity
@XmlRootElement(name = "virtualizationConnector")
@XmlAccessorType(XmlAccessType.FIELD)
public class VirtualizationConnectorDto extends BaseDto {

    @ApiModelProperty(required = true)
    private String name = "";

    @ApiModelProperty(required = true)
    private VirtualizationType type;

    @ApiModelProperty(value = "The Ip of the SDN controller, required if controller is going to be defined")
    private String controllerIP = "";

    @ApiModelProperty(value = "The Username of the SDN controller, required if controller is going to be defined")
    private String controllerUser = "";

    @ApiModelProperty(
            value = "The password of the SDN controller, required if controller is going to be defined")
    private String controllerPassword = "";

    @ApiModelProperty(value = "The Ip of the virtualization provider", required = true)
    private String providerIP = "";

    @ApiModelProperty(value = "The username of the virtualization provider", required = true)
    private String providerUser = "";

    @ApiModelProperty(value = "The password of the virtualization provider")
    private String providerPassword = "";

    @ApiModelProperty(value = "Software Version of provider", allowableValues = "Icehouse Or 5.5")
    private String softwareVersion = "";

    @ApiModelProperty(value = "The SDN controller type", allowableValues = "NONE, NSC")
    private String controllerType;

    @ApiModelProperty(
            value = "The Provider Attributes are all required if Provider is OpenStack except rabbitMQIP if the RabbitMQ endpoint is the same as the OpenStack keystone. "
                    + "This is a map of the following attributes: ishttps, rabbitMQIp, rabbitUser, rabbitMQPassword, rabbitMQPort",
                    allowableValues = "ishttps, rabbitMQIP, rabbitUser, rabbitMQPassword, rabbitMQPort",
                    dataType = "map[string,string]")
    private Map<String, String> providerAttributes = new HashMap<>();

    @ApiModelProperty(value = "Required if Provider is openstack")
    private String adminProjectName;

    @ApiModelProperty(value = "Required if Provider is openstack")
    private String adminDomainId;

    @ApiModelProperty(hidden = true)
    private Set<SslCertificateAttrDto> sslCertificateAttrSet = new HashSet<>();

    /**
     * Gets the controller type
     *
     * @return controller type
     */
    public String getControllerType() {
        return this.controllerType;
    }

    public void setControllerType(String controllerType) {
        this.controllerType = controllerType;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VirtualizationType getType() {
        return this.type;
    }

    public void setType(VirtualizationType type) {
        this.type = type;
    }

    public String getControllerIP() {
        return this.controllerIP;
    }

    public void setControllerIP(String controllerIP) {
        this.controllerIP = controllerIP;
    }

    public String getControllerUser() {
        return this.controllerUser;
    }

    public void setControllerUser(String controllerUser) {
        this.controllerUser = controllerUser;
    }

    public String getControllerPassword() {
        return this.controllerPassword;
    }

    public void setControllerPassword(String controllerPassword) {
        this.controllerPassword = controllerPassword;
    }

    public String getProviderIP() {
        return this.providerIP;
    }

    public void setProviderIP(String providerIP) {
        this.providerIP = providerIP;
    }

    public String getProviderUser() {
        return this.providerUser;
    }

    public void setProviderUser(String providerUser) {
        this.providerUser = providerUser;
    }

    public String getProviderPassword() {
        return this.providerPassword;
    }

    public void setProviderPassword(String providerPassword) {
        this.providerPassword = providerPassword;
    }

    public String getSoftwareVersion() {
        return this.softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public Map<String, String> getProviderAttributes() {
        return this.providerAttributes;
    }

    public void setProviderAttributes(Map<String, String> providerAttributes) {
        this.providerAttributes = providerAttributes;
    }

    public Set<SslCertificateAttrDto> getSslCertificateAttrSet() {
        return this.sslCertificateAttrSet;
    }

    public void setSslCertificateAttrSet(Set<SslCertificateAttrDto> sslCertificateAttrSet) {
        this.sslCertificateAttrSet = sslCertificateAttrSet;
    }

    public String getAdminProjectName() {
        return this.adminProjectName;
    }

    public void setAdminProjectName(String adminProjectName) {
        this.adminProjectName = adminProjectName;
    }

    public String getAdminDomainId() {
        return this.adminDomainId;
    }

    public void setAdminDomainId(String adminDomainId) {
        this.adminDomainId = adminDomainId;
    }

    @ApiModelProperty(hidden = true)
    public boolean isControllerDefined() {
        return (getControllerType() != null && !getControllerType().equals(NO_CONTROLLER_TYPE));
    }

    @Override
    public String toString() {
        return "VirtualizationConnectorDto [name=" + this.name + ", type=" + this.type + ", controllerIP="
                + this.controllerIP + ", controllerUser=" + this.controllerUser + ", providerIP=" + this.providerIP
                + ", providerUser=" + this.providerUser + ", softwareVersion=" + this.softwareVersion + "]";
    }

    public static void sanitizeVirtualizationConnector(VirtualizationConnectorDto dto) {
        dto.setProviderPassword(null);
        dto.setControllerPassword(null);
        dto.getProviderAttributes().put(ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD, null);
    }
}
