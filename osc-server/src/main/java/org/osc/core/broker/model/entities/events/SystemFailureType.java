package org.osc.core.broker.model.entities.events;

public enum SystemFailureType {
    EMAIL_FAILURE("Email Failure"),
    ARCHIVE_FAILURE("Archive Failure"),
    MGR_WEB_SOCKET_NOTIFICATION_FAILURE("Manager Web Socket Notification Failure"),
    OS_NOTIFICATION_FAILURE("Openstack Notification Failure"),
    SCHEDULER_FAILURE("Scheduler Failure"),
    SYSTEM_CLOCK("System Clock Changed"),
    MGR_PROPAGATION_JOB_NOTIFCATION("Manager File Propagation Notification Failure"),
    NSX_NOTIFICATION("NSX Notification Failure");

    private final String text;

    private SystemFailureType(final String text) {
        this.text = text;
    }

    public static SystemFailureType fromText(String text) {
        for (SystemFailureType type : SystemFailureType.values()) {
            if (type.getText().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "No enum constant " + SystemFailureType.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
