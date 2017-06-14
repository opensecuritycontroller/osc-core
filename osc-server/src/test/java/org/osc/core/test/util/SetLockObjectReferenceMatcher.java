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
package org.osc.core.test.util;

import java.util.Set;

import org.mockito.ArgumentMatcher;
import org.osc.core.broker.job.lock.LockObjectReference;

public class SetLockObjectReferenceMatcher extends ArgumentMatcher<Set<LockObjectReference>> {
    private Set<LockObjectReference> expectedLockObjectReferences;

    public SetLockObjectReferenceMatcher(Set<LockObjectReference> expectedLockObjectReferences) {
        this.expectedLockObjectReferences = expectedLockObjectReferences;
    }

    @Override
    public boolean matches(Object object) {
        if (object == null || !(object instanceof Set<?>)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Set<LockObjectReference> lockObjectReferences = (Set<LockObjectReference>) object;

        return this.expectedLockObjectReferences.equals(lockObjectReferences);
    }
}
