package org.osc.core.broker.service.request;

import org.osc.core.broker.service.dto.BaseDto;

public class GetDtoFromEntityRequest extends BaseRequest<BaseDto> {

    private long entityId;
    private long parentId;
    private String entityName;

    public long getEntityId() {
        return this.entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public String getEntityName() {
        return this.entityName;
    }

    public void setEntityName(String entityname) {
        this.entityName = entityname;
    }

    @Override
    public String toString() {
        return "GetDtoFromEntityRequest [entityId=" + this.entityId + ", entityName=" + this.entityName + "]";
    }

    public long getParentId() {
        return this.parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

}
