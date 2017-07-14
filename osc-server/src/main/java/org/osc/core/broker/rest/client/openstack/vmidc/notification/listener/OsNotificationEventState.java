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
    PROJECT_DELETED("identity.project.deleted"),
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
