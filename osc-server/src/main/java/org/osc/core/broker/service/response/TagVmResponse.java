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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "tagVmResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class TagVmResponse implements Response {

    @XmlElement(name = "vmTag")
    public String vmTag;

    public String getVmTag() {
        return vmTag;
    }

    public void setVmTag(String vmTag) {
        this.vmTag = vmTag;
    }

    @Override
    public String toString() {
        return "TagVmResponse [vmTag=" + vmTag + "]";
    }
}

