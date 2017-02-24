package org.osc.core.broker.job.lock;

import java.util.Map;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.lock.LockRequest.LockType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public final class LockManager {

    private static final Logger log = Logger.getLogger(LockManager.class);

    private static LockManager lockManager = new LockManager();

    private LockManager() {
    }

    public static LockManager getLockManager() {
        return lockManager;
    }

    private Map<LockObjectReference, ReadWriteLockRecord> lockMap = Maps.newConcurrentMap();

    /**
     * Try to place a lock on an object. Returns immediately if lock was not
     * acquired.
     *
     * @param lockRequest
     *            The {@link LockRequest}
     * @return true, if the lock acquisition was successful, false otherwise.
     * @throws InterruptedException
     */
    public boolean tryAcquireLock(final LockRequest lockRequest) throws InterruptedException {
        ReadWriteLockRecord lockRecord = getOrAddLockRecord(lockRequest.getObjectRef());
        return lockRecord.tryLock(lockRequest);
    }

    /**
     * Requests a lock on object. Wait timeout duration (or till interruption
     * occurs) till lock can be placed.
     *
     * @param lockRequest
     *            The lock request containing object and lock type.
     * @param timeout
     *            Duration in milliseconds to wait for lock to be acquired.
     * @return True if lock got acquired.
     * @throws InterruptedException
     *             if wait operation was interrupted.
     */
    public boolean acquireLock(final LockRequest lockRequest, long timeout) throws InterruptedException {
        ReadWriteLockRecord lockRecord = getOrAddLockRecord(lockRequest.getObjectRef());
        return lockRecord.lock(lockRequest, timeout);
    }

    /**
     * Requests a lock on object. Wait forever (or till interruption occurs)
     * till lock can be placed.
     *
     * @param lockRequest
     *            The lock request containing object and lock type.
     * @return True if lock got acquired.
     * @throws InterruptedException
     *             if wait operation was interrupted.
     */
    public boolean acquireLock(final LockRequest lockRequest) throws InterruptedException {
        return acquireLock(lockRequest, 0);
    }

    /**
     * Releases the lock held on the object in the request.
     *
     * @param lockRequest
     *            The {@link LockRequest}
     */
    public synchronized void releaseLock(LockRequest lockRequest) {
        ReadWriteLockRecord lockRecord = getLockRecord(lockRequest.getObjectRef());
        if (lockRecord == null) {
            log.warn("Release lock requested but no active locks found for object "
                    + lockRequest.getObjectRef().getId());
            return;
        }

        lockRecord.unlock(lockRequest);
        if (lockRecord.getLockType() == LockType.UNKNOWN_LOCK && lockRecord.getWaitingWriters() == 0
                && lockRecord.getReadLockCount() == 0 && lockRecord.getWaitingReaders() == 0) {
            this.lockMap.remove(lockRequest.getObjectRef());
        }
    }

    /**
     * Attempts to upgrade a read lock on an object to a write lock. Waits until it can
     * upgrade the lock.
     *
     * @param lockRequest
     *            {@link LockRequest} for the object to be upgraded.
     * @return true, if the read lock was successfully changed to a write lock,
     *         false otherwise.
     * @throws InterruptedException
     */
    public boolean upgradeLockWithWait(LockRequest lockRequest) throws InterruptedException {
        ReadWriteLockRecord lockRecord = getLockRecord(lockRequest.getObjectRef());
        if (lockRecord == null) {
            log.info("Upgrade lock requested but no active locks found for object "
                    + lockRequest.getObjectRef().getId());
            return false;
        }

        return lockRecord.upgradeLockWithWait(lockRequest);
    }

    /**
     * Converts a write lock on an object to a read lock.
     *
     * @param lockRequest
     *            {@link LockRequest} for an object whose write lock must be
     *            downgraded to a read lock.
     * @return true, if write lock to read lock downgrade request was
     *         successful, false otherwise.
     * @throws InterruptedException
     */
    public boolean downgradeLock(LockRequest lockRequest) throws InterruptedException {
        ReadWriteLockRecord lockRecord = getLockRecord(lockRequest.getObjectRef());
        if (lockRecord == null) {
            log.info("Downgrade lock requested but no active locks found for object "
                    + lockRequest.getObjectRef().getId());
            return false;
        }
        return lockRecord.downgradeLock(lockRequest);
    }

    private ReadWriteLockRecord getLockRecord(LockObjectReference objectRef) {
        return this.lockMap.get(objectRef);
    }

    private synchronized ReadWriteLockRecord getOrAddLockRecord(LockObjectReference objectRef) {
        ReadWriteLockRecord lockRecord = getLockRecord(objectRef);
        if (lockRecord == null) {
            /*
             * Insert a new lock record, if one doesn't already exist for this
             * object.
             */
            lockRecord = new ReadWriteLockRecord();
            this.lockMap.put(objectRef, lockRecord);
        }
        return lockRecord;
    }

    /**
     * Get a immutable copy of all active lock requests currently holding a lock
     * on a particular object.
     *
     * @param objectRef
     *            The object for which active lock requests are being returned.
     * @return a immutable map containing the current requests holding locks of
     *         an object
     */
    public ImmutableMap<Task, LockRequest> getLockRequests(LockObjectReference objectRef) {
        ReadWriteLockRecord lockRecord = getLockRecord(objectRef);

        return lockRecord == null ? null : lockRecord.getLockRequests();
    }

    public ImmutableMap<LockObjectReference, ReadWriteLockRecord> getLockInformation() {
        return ImmutableMap.copyOf(this.lockMap);
    }
}
