package org.osc.core.broker.job;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**

 *         TaskInput defines an annotation which marks a {@link Task} member
 *         field as an input to be assigned by one of the task ancestors.
 * 
 *         Task member annotated with TaskInput must be public.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TaskInput {
}
