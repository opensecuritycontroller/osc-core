package org.osc.core.broker.rest.client.nsx.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "basicinfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class BasicInfo {

    private String objectId;
    private String objectTypeName;
    private String vsmUuid;
    private String name;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getVsmUuid() {
        return vsmUuid;
    }

    public void setVsmUuid(String vsmUuid) {
        this.vsmUuid = vsmUuid;
    }

    public String getObjectTypeName() {
        return objectTypeName;
    }

    public void setObjectTypeName(String objectTypeName) {
        this.objectTypeName = objectTypeName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s [Id: %s, Name: %s, objectTypeName: %s, vsmUuid: %s]", this.getClass().getSimpleName(),
                objectId, name, objectTypeName, vsmUuid);
    }
}
