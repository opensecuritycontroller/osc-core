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
package org.osc.core.test.util.mockito.matchers;

import org.mockito.ArgumentMatcher;
import org.osc.sdk.controller.element.Element;

/**
 * Matches the element using the element id.
 *
 * @param any subclass of Element
 */
public class ElementIdMatcher<T extends Element> extends ArgumentMatcher<T> {
    private String id;

    public ElementIdMatcher(String id) {
        this.id = id;
    }

    @Override
    public boolean matches(Object object) {
        if (object == null || !(object instanceof Element)) {
            return false;
        }

        Element element = (Element) object;
        return (element.getElementId() == null && this.id == null) || this.id.equals(element.getElementId());
    }
}
