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
