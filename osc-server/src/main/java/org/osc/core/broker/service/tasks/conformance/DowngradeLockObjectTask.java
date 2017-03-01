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
package org.osc.core.broker.service.tasks.conformance;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DowngradeLockObjectTask extends TransactionalTask {

    private LockRequest lockRequest;

    public DowngradeLockObjectTask(LockRequest lockRequest) {
        this.lockRequest = lockRequest;
    }

    @Override
    public String getName() {
        return "Downgrade To '" + this.lockRequest.getLockType() + "' for Lock Object '"
                + this.lockRequest.getObjectRef().getName() + "' (" + this.lockRequest.getObjectRef().getType() + ")";
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        LockManager.getLockManager().downgradeLock(this.lockRequest);
    }

}
