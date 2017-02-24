package org.osc.core.broker.service.request;

public class DeleteUserRequest implements Request {
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
