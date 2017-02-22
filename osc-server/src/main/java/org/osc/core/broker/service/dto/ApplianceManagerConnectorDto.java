package org.osc.core.broker.service.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.job.JobState;
import org.osc.core.broker.job.JobStatus;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.util.ValidateUtil;

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
    private JobState lastJobState;

    @ApiModelProperty(readOnly = true)
    private JobStatus lastJobStatus;

    @ApiModelProperty(readOnly = true)
    private Long lastJobId;

    @ApiModelProperty(readOnly = true, value = "Determines whether the appliance manager supports policy mapping.")
    private Boolean isPolicyMappingSupported;

    @ApiModelProperty(value = "The Api key of the Manager required if Manager uses Api key Authentication.")
    private String apiKey;

    @ApiModelProperty(hidden = true)
    private Set<SslCertificateAttr> sslCertificateAttrSet = new HashSet<>();

    @ApiModelProperty(readOnly = true)
    private String vendorName;

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

    public JobStatus getLastJobStatus() {
        return this.lastJobStatus;
    }

    public void setLastJobStatus(JobStatus lastJobStatus) {
        this.lastJobStatus = lastJobStatus;
    }

    public JobState getLastJobState() {
        return this.lastJobState;
    }

    public void setLastJobState(JobState lastJobState) {
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

    public Set<SslCertificateAttr> getSslCertificateAttrSet() {
        return this.sslCertificateAttrSet;
    }

    public void setSslCertificateAttrSet(Set<SslCertificateAttr> sslCertificateAttrSet) {
        this.sslCertificateAttrSet = sslCertificateAttrSet;
    }

    public String getVendorName() {
        return this.vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    @Override
    public String toString() {
        return "ApplianceManagerConnectorDto [id=" + getId() + ", name=" + this.name + ", managerType="
                + this.managerType + ", ipAddress=" + this.ipAddress + ", username=" + this.username
                + ", password=***** ]";
    }

    /**
     * Based on the type of DTO makes sure the required fields are not null and the fields which should
     * not be specified for the type are null.
     *
     * @param dto
     *            the dto
     * @throws Exception
     *             in case the required fields are null or fields which should
     *             NOT be specified are specified
     */
    public static void checkForNullFields(ApplianceManagerConnectorDto dto, boolean skipPasswordNullCheck)
            throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> notNullFieldsMap = new HashMap<String, Object>();
        Map<String, Object> nullFieldsMap = new HashMap<String, Object>();

        notNullFieldsMap.put("Name", dto.getName());
        notNullFieldsMap.put("Type", dto.getManagerType());
        notNullFieldsMap.put("IP Address", dto.getIpAddress());
        if (ManagerApiFactory.isKeyAuth(dto.getManagerType()) && !skipPasswordNullCheck) {
            notNullFieldsMap.put("API Key", dto.getApiKey());
        } else if (ManagerApiFactory.isBasicAuth(dto.getManagerType())) {
            if (!skipPasswordNullCheck) {
                notNullFieldsMap.put("Password", dto.getPassword());
            }
            notNullFieldsMap.put("User Name", dto.getUsername());

            nullFieldsMap.put("API Key", dto.getApiKey());
        }
        ValidateUtil.checkForNullFields(notNullFieldsMap);
        ValidateUtil.validateFieldsAreNull(nullFieldsMap);
    }

    public static void checkForNullFields(ApplianceManagerConnectorDto dto) throws Exception {
        checkForNullFields(dto, false);
    }

    public static void checkFieldLength(ApplianceManagerConnectorDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("Name", dto.getName());
        if (ManagerApiFactory.isBasicAuth(dto.getManagerType())) {
            map.put("Password", dto.getPassword());
            map.put("User Name", dto.getUsername());
        }
        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }

    public static void sanitizeManagerConnector(ApplianceManagerConnectorDto dto) {
        dto.setPassword(null);
        dto.setApiKey(null);
    }
}
