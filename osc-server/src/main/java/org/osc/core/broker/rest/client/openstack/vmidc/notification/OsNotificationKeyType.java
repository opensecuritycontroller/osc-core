package org.osc.core.broker.rest.client.openstack.vmidc.notification;

/**
 * 
 * This enum provides list of Keys we can parser Incoming Notification message on (i.e. "instance_id", "device_id"
 * etc..) those objects
 * 
 */

public enum OsNotificationKeyType {

    INSTANCE_ID("instance_id"),
    NETWORK_ID("network_id"),
    DEVICE_ID("device_id"),
    AGGREGRATE_ID("aggregate_id"),
    RESOURCE_INFO("resource_info"),
    CONTEXT_TENANT_ID("_context_tenant_id"),
    TENANT_ID("tenant_id"),
    PORT_ID("port_id"),
    SUBNET_ID("subnet_id"),
    FIXED_IPS("fixed_ips"),
    DEVICE_OWNER("device_owner");

    private final String text;

    private OsNotificationKeyType(final String text) {
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
