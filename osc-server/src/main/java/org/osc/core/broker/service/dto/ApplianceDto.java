package org.osc.core.broker.service.dto;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.util.ValidateUtil;

// Appliance Data Transfer Object associated with Appliance Entity
@XmlRootElement(name = "appliance")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplianceDto extends BaseDto {

    private String model;
    private ManagerType managerType;
    private String managerVersion;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public ManagerType getManagerType() {
        return managerType;
    }

    public void setManagerType(ManagerType managerType) {
        this.managerType = managerType;
    }

    public String getManagerVersion() {
        return managerVersion;
    }

    public void setManagerVersion(String managerVersion) {
        this.managerVersion = managerVersion;
    }

    @Override
    public String toString() {
        return "ApplianceDto [id=" + getId() + ", model=" + model + ", managerType=" + managerType + ", managerVersion="
                + managerVersion + "]";
    }

    public static void checkForNullFields(ApplianceDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Appliance Model", dto.getModel());
        map.put("Appliance Manager Type", dto.getManagerType());
        map.put("Appliance Manager Version", dto.getManagerVersion());

        ValidateUtil.checkForNullFields(map);
    }

    public static void checkFieldLength(ApplianceDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("Appliance Model", dto.getModel());
        map.put("Appliance Manager Version", dto.getManagerVersion());

        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }
}
