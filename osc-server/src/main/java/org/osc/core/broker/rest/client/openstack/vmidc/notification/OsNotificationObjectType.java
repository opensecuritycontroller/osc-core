package org.osc.core.broker.rest.client.openstack.vmidc.notification;

/**
 *
 * This enum provides mapping between Open stack objects (i.e. VM, Port etc..) and Open stack Standard Service managing
 * those objects
 *
 */
public enum OsNotificationObjectType {

    TENANT("identity"),
    VM("compute"),
    PORT("port"),
    NETWORK("network"),
    HOST_AGGREGRATE("aggregate"),
    FLOATING_IP("floatingip"),
    SCHEDULER("scheduler"),
    ROUTER("router");

    private final String text;

    private OsNotificationObjectType(final String text) {
        this.text = text;
    }

    public static OsNotificationObjectType getType(String text) {
        for (OsNotificationObjectType type : OsNotificationObjectType.values()) {
            if (text.startsWith(type.toString())) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant " + OsNotificationObjectType.class.getCanonicalName()
                + " Found for " + text);
    }

    @Override
    public String toString() {
        return this.text;
    }
}
