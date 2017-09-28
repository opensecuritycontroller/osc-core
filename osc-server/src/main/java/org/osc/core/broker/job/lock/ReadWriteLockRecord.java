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

import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

class ReadWriteLockRecord {

    private static final Logger log = LoggerFactory.getLogger(ReadWriteLockRecord.class);

    private LockType lockType = LockType.UNKNOWN_LOCK;

    /*
     * Hold all locks requests placed on this lock record the object mapped by their unlock task
     */
    private Map<Task, LockRequest> lockRequestsMap = Maps.newConcurrentMap();

    /*
     * Count current reader count
     */
    private int readLockCount = 0;
    /*
     * Count write lock waiters
     */
    private int waitingWriters = 0;
    /*
     * Count read lock waiters
     */
    private int waitingReaders = 0;

    public ReadWriteLockRecord() {
        // Initially, lock type is unknown till first lock is successfully placed.
        this.lockType = LockType.UNKNOWN_LOCK;
        this.readLockCount = 0;
        this.waitingWriters = 0;
        this.waitingReaders = 0;
    }

    /**
     * Try to gain a lock as defined by {@link LockRequest}
     *
     * @param lockRequest
     *            The {@link LockRequest} lock request.
     * @return true if lock was acquired successfully. False if otherwise.
     * @throws InterruptedException
     */
    public synchronized boolean tryLock(LockRequest lockRequest) throws InterruptedException {
        log.info("tryLock " + lockRequest);

        if (lockRequest.getLockType() == LockType.READ_LOCK) {
            return getReadLock(lockRequest, true, 0);
        } else {
            return getWriteLock(lockRequest, true, 0);
        }
    }

    public synchronized boolean lock(LockRequest lockRequest) throws InterruptedException {
        return lock(lockRequest, 0);
    }

    /**
     * Gain a lock of type defined in the {@link LockRequest}
     *
     * @param lockRequest
     *            The {@link LockRequest} lock request.
     * @throws InterruptedException
     */
    public synchronized boolean lock(LockRequest lockRequest, long timeout) throws InterruptedException {
        log.info("Lock " + lockRequest);

        if (lockRequest.getLockType() == LockType.READ_LOCK) {
            return getReadLock(lockRequest, false, timeout);
        } else {
            return getWriteLock(lockRequest, false, timeout);
        }
    }

    private synchronized boolean getReadLock(LockRequest lockRequest, boolean tryLock, long timeout) {
        log.info("Waiting readers: " + this.waitingReaders);
        this.waitingReaders++;

        DateTime start = new DateTime();
        long remainingWait = timeout;

        while (this.lockType == LockType.WRITE_LOCK) {
            if (tryLock) {
                this.waitingReaders--;
                return false;
            } else if (timeout > 0) {
                if (new DateTime().getMillis() - start.getMillis() >= timeout) {
                    this.waitingWriters--;
                    return false;
                } else {
                    remainingWait = timeout - (new DateTime().getMillis() - start.getMillis());
                }
            }

            try {
                if (timeout > 0) {
                    wait(remainingWait);
                } else {
                    wait();
                }
            } catch (InterruptedException e) {
                this.waitingReaders--;
                throw new RuntimeException(e);
            }
        }

        this.waitingReaders--;
        this.readLockCount++;

        log.info("Gained Read lock " + lockRequest);
        setLockType(LockType.READ_LOCK);
        this.lockRequestsMap.put(lockRequest.getUnlockTask(), lockRequest);
        return true;
    }

    private synchronized boolean getWriteLock(LockRequest lockRequest, boolean tryLock, long timeout)
            throws InterruptedException {
        this.waitingWriters++;

        DateTime start = new DateTime();
        long remainingWait = timeout;

        while (this.lockType == LockType.WRITE_LOCK || this.readLockCount > 0) {
            log.info("Current read lock count: " + this.readLockCount);

            if (tryLock) {
                this.waitingWriters--;
                return false;
            } else if (timeout > 0) {
                if (new DateTime().getMillis() - start.getMillis() >= timeout) {
                    log.info("Waiting for lock " + lockRequest + " timed out.");
                    this.waitingWriters--;
                    return false;
                } else {
                    remainingWait = timeout - (new DateTime().getMillis() - start.getMillis());
                }
            }

            try {
                log.info("Waiting for lock " + lockRequest);
                if (timeout > 0) {
                    wait(remainingWait);
                } else {
                    wait();
                }
                log.info("Notified of lock release " + lockRequest);
            } catch (InterruptedException e) {
                this.waitingWriters--;
                throw e;
            }
        }

        log.info("Gained Write lock " + lockRequest);
        this.waitingWriters--;
        setLockType(LockType.WRITE_LOCK);
        this.lockRequestsMap.put(lockRequest.getUnlockTask(), lockRequest);
        return true;
    }

    /**
     * Remove lock of type defined by lock request
     *
     * @param lockRequest
     *            The {@link LockRequest} used to lock the object
     */
    public synchronized void unlock(LockRequest lockRequest) {
        log.info("unlock " + lockRequest);

        /*
         * Verify this request holds an active lock.
         */
        LockRequest currentLockRequest = this.lockRequestsMap.get(lockRequest.getUnlockTask());
        if (currentLockRequest == null || !currentLockRequest.equals(lockRequest)) {
            log.warn("unlock " + lockRequest + " has not lock record!!");
            return;
        }

        if (this.lockType == LockType.READ_LOCK) {
            /*
             * This is a reader lock. Just decrease readers lock count
             */
            this.readLockCount--;

            /*
             * Is last read lock removed?
             */
            if (this.readLockCount == 0) {
                setLockType(LockType.UNKNOWN_LOCK);
            }
        } else {
            /*
             * This is a write lock. Just reset lock type.
             */
            setLockType(LockType.UNKNOWN_LOCK);
        }

        this.lockRequestsMap.remove(lockRequest.getUnlockTask());
        notifyAll();
    }

    /**
     * Attempts to upgrade a read lock on an object to a write lock. This
     * request should hold at list an active read lock to the object in question
     * before hand. If write lock is already acquired, this will translate to
     * no-op. Waits until it can upgrade the lock.
     *
     * @param lockRequest
     *            {@link LockRequest} for the object to be upgraded.
     * @return true, if there are no read locks other then this request and the
     *         request happen to hold the only read lock, false otherwise. If
     *         request already poses a write lock, true is returned.
     * @throws InterruptedException
     */
    public synchronized boolean upgradeLockWithWait(LockRequest lockRequest) throws InterruptedException {
        log.info("Upgrade lock with wait requested " + lockRequest);
        LockRequest currentLockRequest = this.lockRequestsMap.get(lockRequest.getUnlockTask());
        /*
         * If this request does not currently poses an active read lock we
         * cannot upgrade it.
         */
        if (currentLockRequest == null || !currentLockRequest.equals(lockRequest)) {
            return false;
        }
        /*
         * If this lock request already holds a write lock, no further action
         * required.
         */
        if (this.lockType == LockType.WRITE_LOCK) {
            return true;
        }

        unlock(lockRequest);
        lockRequest.setLockType(LockType.WRITE_LOCK);
        return lock(lockRequest);
    }

    /**
     * Converts a write lock on an object to a read lock. This request must hold
     * an active write lock to the object in question.
     *
     * @param lockRequest
     *            {@link LockRequest} for an object whose write lock is to be
     *            downgraded to a read lock.
     * @return true, if the object is write locked by this request, false
     *         otherwise.
     * @throws InterruptedException
     */
    public synchronized boolean downgradeLock(LockRequest lockRequest) throws InterruptedException {
        log.info("downgrade lock requested " + lockRequest);

        /*
         * Check current lock type. If it is not write lock, no point to
         * continue.
         */
        if (this.lockType != LockType.WRITE_LOCK) {
            return false;
        }

        LockRequest currentlockRequest = this.lockRequestsMap.get(lockRequest.getUnlockTask());
        /*
         * Check if this request is the current owner of this write lock. If
         * not, no point of continuing.
         */
        if (currentlockRequest == null || !currentlockRequest.equals(lockRequest)) {
            return false;
        }

        unlock(lockRequest);
        lockRequest.setLockType(LockType.READ_LOCK);
        return tryLock(lockRequest);
    }

    public final synchronized ImmutableMap<Task, LockRequest> getLockRequests() {
        return ImmutableMap.copyOf(this.lockRequestsMap);
    }

    /**
     * @return the waiting readers count
     */
    public synchronized int getWaitingReaders() {
        return this.waitingReaders;
    }

    /**
     * @return the waiting writers count
     */
    public synchronized int getWaitingWriters() {
        return this.waitingWriters;
    }

    public synchronized void setLockType(LockType lockType) {
        this.lockType = lockType;
    }

    /**
     * @return the current lock type
     */
    public synchronized LockType getLockType() {
        return this.lockType;
    }

    /**
     * Get current number of read locks
     *
     * @return read lock count
     */
    public synchronized int getReadLockCount() {
        return this.readLockCount;
    }

    /**
     * @return true if current lock type is write lock. False if otherwise.
     */
    public synchronized boolean isWriteLocked() {
        return this.lockType == LockType.WRITE_LOCK;
    }

    /**
     * @return true if there are any locks placed. False if otherwise.
     */
    public synchronized boolean isLocked() {
        return this.lockType != LockType.UNKNOWN_LOCK;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<Task, LockRequest> entry : this.lockRequestsMap.entrySet()) {
            sb.append(entry.getValue() + "\n");
        }

        return "ReadWriteLockRecord [lockType=" + this.lockType + ", readLockCount=" + this.readLockCount
                + ", waitingWriters=" + this.waitingWriters + ", waitingReaders=" + this.waitingReaders + "\nLocks:\n"
                + sb.toString() + "\n]";
    }

}
