package org.geysermc.connector.network.translators.block.entity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface BlockEntity {
    String name();
    boolean delay();
}
