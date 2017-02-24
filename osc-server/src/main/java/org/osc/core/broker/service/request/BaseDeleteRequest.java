package org.osc.core.broker.service.request;

public class BaseDeleteRequest extends BaseIdRequest {
    private boolean forceDelete = false; // default false

    public BaseDeleteRequest(long id, long parentId, boolean forceDelete) {
        super(id, parentId);
        this.forceDelete = forceDelete;
    }

    public BaseDeleteRequest(long id) {
        super(id);
    }

    public BaseDeleteRequest(long id, boolean forceDelete) {
        super(id);
        this.forceDelete = forceDelete;
    }

    public BaseDeleteRequest() {
        super();
    }

    public boolean isForceDelete() {
        return this.forceDelete;
    }

    public void setForceDelete(boolean isForceDeleted) {
        this.forceDelete = isForceDeleted;
    }

}
