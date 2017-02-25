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
package org.osc.core.broker.job.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "lockInformation")
@XmlAccessorType(XmlAccessType.FIELD)
public class LockInformationDto {

    private List<ReadWriteLockRecordDto> lockRecordInformation = new ArrayList<>();

    LockInformationDto() {
    }

    public LockInformationDto(Map<LockObjectReference, ReadWriteLockRecord> lockInformation) {
        for (Entry<LockObjectReference, ReadWriteLockRecord> entry : lockInformation.entrySet()) {
            this.lockRecordInformation.add(new ReadWriteLockRecordDto(entry.getValue()));
        }
    }

}
