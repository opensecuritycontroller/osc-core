/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
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
 * This enum provides mapping between Open stack objects (i.e. VM, Port etc..) and Open stack Standard Service managing
 * those objects
 *
 */
public enum OsNotificationObjectType {

    PROJECT("identity"),
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
