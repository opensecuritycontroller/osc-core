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
package org.osc.core.broker.rest.client.nsx.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class DeployedServices {

    @XmlElement(name = "deployedService")
    private List<DeployedService> list;

    public List<DeployedService> getList() {
        return list;
    }

    public void setList(List<DeployedService> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        if (list == null) {
            return super.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (DeployedService ds : list) {
            sb.append(ds.toString() + "\n");
        }
        return sb.toString();
    }
}
