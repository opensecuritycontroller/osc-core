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
package org.osc.core.test.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;

import org.apache.commons.lang.NullArgumentException;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskNode;

/**
 *  Encapsulates the functionality for comparing task nodes.
 */
public class TaskNodeComparer {

    public static void compare(TaskNode tn1, TaskNode tn2) throws Exception {
        if ((tn1 == null) != (tn2 == null)) {
            throw new NullArgumentException("Both task nodes should be null or neither should be.");
        }

        if (tn1 == null || tn2 == null) {
            return;
        }

        if (!tn1.getTaskGaurd().equals(tn2.getTaskGaurd())) {
            throw new IllegalStateException(MessageFormat.format("The task node {0} has task guard {1} but the task node {2} has {3}",
                    tn1.getName(), tn1.getTaskGaurd(), tn2.getName(), tn2.getTaskGaurd()));
        }

        Task t1 = tn1.getTask();
        Task t2 = tn2.getTask();

        if ((t1 == null) != (t2 == null)) {
            throw new NullArgumentException("Both tasks should be null or neither should be.");
        }

        if (t1 == null || t2 == null) {
            return;
        }

        if (t1.getClass() != t2.getClass()) {
            throw new IllegalStateException(MessageFormat.format("The task {0} is of type {1} but the task {2} is {3}",
                    t1.getName(), t1.getClass().getName(), t2.getName(), t2.getClass().getName()));
        }

        Field[] t1Fields = t1.getClass().getDeclaredFields();
        if (t1Fields == null) {
            return;
        }

        for (Field t1Field : t1Fields) {
            if (!Modifier.isFinal(t1Field.getModifiers())) {
                t1Field.setAccessible(true);
                Object t1FieldValue = t1Field.get(t1);

                Field t2Field = t2.getClass().getDeclaredField(t1Field.getName());
                t2Field.setAccessible(true);
                Object t2FieldValue = t2Field.get(t2);

                if (t1FieldValue == null && t2FieldValue == null) {
                    continue;
                }

                if (!t1FieldValue.equals(t2FieldValue)) {
                    throw new IllegalStateException(MessageFormat.format("The task {0} has the field {1} with value {2}, but the expected value was {3}",
                            t1.getName(), t1Field.getName(), t1FieldValue, t2FieldValue));
                }
            }
        }
    }


}
