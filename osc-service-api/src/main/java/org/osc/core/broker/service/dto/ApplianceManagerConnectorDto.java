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

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.common.manager.ManagerType;

import io.swagger.annotations.ApiModelProperty;

// Appliance Manager Connector Data Transfer Object associated with MC Entity
@XmlRootElement(name = "applianceManagerConnector")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplianceManagerConnectorDto extends BaseDto {

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = true)
    private ManagerType managerType;

    @ApiModelProperty(required = true)
    private String ipAddress;

    @ApiModelProperty(value = "The username of the Manager required if Manager uses Password Authentication.")
    private String username;

    @ApiModelProperty(value = "The password of the Manager required if Manager uses Password Authentication.")
    private String password;

    @ApiModelProperty(readOnly = true)
    private String lastJobState;

    @ApiModelProperty(readOnly = true)
    private String lastJobStatus;

    @ApiModelProperty(readOnly = true)
    private Long lastJobId;

    @ApiModelProperty(readOnly = true, value = "Determines whether the appliance manager supports policy mapping.")
    private Boolean isPolicyMappingSupported;

    @ApiModelProperty(value = "The Api key of the Manager required if Manager uses Api key Authentication.")
    private String apiKey;

    @ApiModelProperty(hidden = true)
    private Set<SslCertificateAttrDto> sslCertificateAttrSet = new HashSet<>();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ManagerType getManagerType() {
        return this.managerType;
    }

    public void setManagerType(ManagerType type) {
        this.managerType = type;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLastJobStatus() {
        return this.lastJobStatus;
    }

    public void setLastJobStatus(String lastJobStatus) {
        this.lastJobStatus = lastJobStatus;
    }

    public String getLastJobState() {
        return this.lastJobState;
    }

    public void setLastJobState(String lastJobState) {
        this.lastJobState = lastJobState;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Long getLastJobId() {
        return this.lastJobId;
    }

    public void setLastJobId(Long lastJobId) {
        this.lastJobId = lastJobId;
    }

    public Boolean isPolicyMappingSupported() {
        return this.isPolicyMappingSupported;
    }

    public void setPolicyMappingSupported(Boolean isPolicyMappingSupported) {
        this.isPolicyMappingSupported = isPolicyMappingSupported;
    }

    public Set<SslCertificateAttrDto> getSslCertificateAttrSet() {
        return this.sslCertificateAttrSet;
    }

    public void setSslCertificateAttrSet(Set<SslCertificateAttrDto> sslCertificateAttrSet) {
        this.sslCertificateAttrSet = sslCertificateAttrSet;
    }

    @Override
    public String toString() {
        return "ApplianceManagerConnectorDto [id=" + getId() + ", name=" + this.name + ", managerType="
                + this.managerType + ", ipAddress=" + this.ipAddress + ", username=" + this.username
                + ", password=***** ]";
    }

    public static void sanitizeManagerConnector(ApplianceManagerConnectorDto dto) {
        dto.setPassword(null);
        dto.setApiKey(null);
    }
}
