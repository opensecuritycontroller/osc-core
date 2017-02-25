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
package org.osc.core.broker.service.tasks.conformance;

import java.util.Set;

import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.service.tasks.BaseTask;

public class UnlockObjectTask extends BaseTask {

    private LockObjectReference objectRef;
    private LockType lockType = LockType.UNKNOWN_LOCK;

    public UnlockObjectTask(LockObjectReference objectRef, LockType lockType) {
        super(getName(objectRef, lockType));
        this.lockType = lockType;
        this.objectRef = objectRef;
    }

    public LockObjectReference getObjectRef() {
        return this.objectRef;
    }

    public LockType getLockType() {
        return this.lockType;
    }

    public void setLockType(LockType lockType) {
        this.lockType = lockType;
    }

    private static String getName(LockObjectReference objectRef, LockType lockType) {
        return "Unlock object '" + objectRef.getName() + "' (" + lockType + ":" + objectRef.getType() + ")";
    }

    @Override
    public void execute() throws Exception {
        LockManager.getLockManager().releaseLock(new LockRequest(this));
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) + " [" + this.objectRef + "], ["
                + this.lockType + "]";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return null;
    }

}
