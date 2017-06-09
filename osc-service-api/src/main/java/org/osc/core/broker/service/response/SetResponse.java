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
package org.osc.core.broker.service.response;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Generic response object which contains a List of objects
 *
 * @param <T> the type of objects the list contains
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SetResponse<T> implements Response {

    private Set<T> set = new HashSet<T>();

    public SetResponse() {
    }

    public SetResponse(Set<T> set) {
        this.set = set;
    }

    public Set<T> getSet() {
        return this.set;
    }

    public void setSet(Set<T> set) {
        this.set = set;
    }



}
