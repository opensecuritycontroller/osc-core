package org.osc.core.broker.service.request;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.util.ValidateUtil;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseIdRequest extends BaseRequest<BaseDto> {
    private Long id;
    private Long parentId;

    public BaseIdRequest(long id, long parentId) {
        this.id = id;
        this.parentId = parentId;
    }

    public BaseIdRequest(long id) {
        this.id = id;
        this.parentId = null; // parent unassigned
    }

    public BaseIdRequest() {
        this.id = null; // id unassigned
        this.parentId = null; // parent unassigned
    }

    public Long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public static void checkForNullId(BaseIdRequest req) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("Id", req.getId());
        ValidateUtil.checkForNullFields(map);
    }

    public static void checkForNullIdAndParentNullId(BaseIdRequest req) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("Id", req.getId());
        map.put("Parent Id", req.getParentId());
        ValidateUtil.checkForNullFields(map);
    }

}
