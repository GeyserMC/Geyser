package org.geysermc.connector.network.translators.block;

import com.nukkitx.nbt.tag.CompoundTag;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Some block entities need to be loaded in later
 */
@Retention(value = RetentionPolicy.RUNTIME)
public @interface LoadLater {
    String identifier();
}
