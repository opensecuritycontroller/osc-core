package org.osc.core.broker.service.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name ="domain")
@XmlAccessorType(XmlAccessType.FIELD)
public class DomainDto extends BaseDto {

    @ApiModelProperty(required = true)
    private String name;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DomainDto [id=" + getId() + ", name=" + this.name + "]";
    }

}
