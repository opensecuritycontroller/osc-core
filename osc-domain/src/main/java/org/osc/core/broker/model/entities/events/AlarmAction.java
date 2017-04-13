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
package org.osc.core.broker.model.entities.events;

public enum AlarmAction {

    NONE("None"), EMAIL("Email");

    private final String text;

    private AlarmAction(final String text) {
        this.text = text;
    }

    public static AlarmAction fromText(String text) {
        for (AlarmAction action : AlarmAction.values()) {
            if (action.getText().equals(text)) {
                return action;
            }
        }
        throw new IllegalArgumentException("No enum constant " + AlarmAction.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return text;
    }
}
