package org.osc.core.broker.service.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

/**
 * Default Implementation of a Response. Any common information for responses can be added
 * in here to avoid code duplication.
 */
@XmlRootElement(name ="response")
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseResponse implements Response {

    @ApiModelProperty(value = "Id of the entity", readOnly = true, required = true)
    private Long id;

    public BaseResponse() {
    }

    public BaseResponse(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
