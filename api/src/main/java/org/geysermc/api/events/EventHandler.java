package org.geysermc.api.events;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation to put on all methods that are events.
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    /**
     * @return the order to execute events.
     * @see EventPriority
     */
    EventPriority value() default EventPriority.NORMAL;

    /**
     * When an eventHandler should be run.
     * The names mostly explain.
     */
    enum EventPriority {
        FIRST,
        NORMAL,
        LAST,
        READ_ONLY;
    }
}
