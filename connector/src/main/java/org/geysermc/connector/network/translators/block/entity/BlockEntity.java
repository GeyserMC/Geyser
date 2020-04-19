package org.geysermc.connector.network.translators.block.entity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface BlockEntity {
    /**
     * Whether to delay the sending of the block entity
     */
    boolean delay();
    /**
     * The block entity name
     */
    String name();
    /**
     * The search term used in BlockTranslator
     */
    String regex();
}
