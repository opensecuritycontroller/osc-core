package org.osc.core.broker.job;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**

 *         TaskOutput defines an annotation which marks a {@link Task} member
 *         field as an output for descendant tasks.
 * 
 *         Task member annotated with TaskInput must be public.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TaskOutput {
}
