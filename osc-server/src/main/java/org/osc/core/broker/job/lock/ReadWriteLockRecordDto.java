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
package org.osc.core.broker.job.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.job.Task;

@XmlRootElement(name = "lockRecord")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReadWriteLockRecordDto {

    private List<String> lockRequests = new ArrayList<>();
    private int readLockCount = 0;
    private int waitingWriters = 0;
    private int waitingReaders = 0;

    ReadWriteLockRecordDto() {
    }

    public ReadWriteLockRecordDto(ReadWriteLockRecord lockRecord) {
        for (Entry<Task, LockRequest> lockEntry : lockRecord.getLockRequests().entrySet()) {
            this.lockRequests.add(lockEntry.getKey().toString());
        }
        this.readLockCount = lockRecord.getReadLockCount();
        this.waitingWriters = lockRecord.getWaitingWriters();
        this.waitingReaders = lockRecord.getWaitingReaders();
    }

    public List<String> getLockRequests() {
        return this.lockRequests;
    }

    public int getReadLockCount() {
        return this.readLockCount;
    }

    public int getWaitingWriters() {
        return this.waitingWriters;
    }

    public int getWaitingReaders() {
        return this.waitingReaders;
    }

}
