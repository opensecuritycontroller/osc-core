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
