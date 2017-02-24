package org.osc.core.rest.client.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * An annotation to specify that we should hide the information from logging. If you have nested fields which are hidden
 * the full hierarchy should have this annotation specified
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VmidcLogHidden {

}
