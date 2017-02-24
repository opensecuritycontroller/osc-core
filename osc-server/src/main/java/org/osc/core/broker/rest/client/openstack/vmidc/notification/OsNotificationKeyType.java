/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
