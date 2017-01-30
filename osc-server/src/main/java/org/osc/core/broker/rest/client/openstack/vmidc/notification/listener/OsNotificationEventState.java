package org.osc.core.broker.rest.client.openstack.vmidc.notification.listener;

public enum OsNotificationEventState {

    // TODO: Future. Openstack. Later we might add new event states. For now only care out end state on every event

    CREATE("create.end"),
    UPDATE("update"),
    DELETE("delete.end"),
    INTERFACE_DELETE("interface.delete"),
    UPDATE_PROP("updateprop.end"),
    REMOVE_HOST("removehost.end"),
    ADD_HOST("addhost.end"),
    POWER_OFF("power_off.end"),
    POWER_ON("power_on.end"),
    TENANT_DELETED("identity.project.deleted"),
    UPDATE_END("update.end"),
    RESIZE_CONFIRM_END("compute.instance.resize.confirm.end");

    private final String text;

    private OsNotificationEventState(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
