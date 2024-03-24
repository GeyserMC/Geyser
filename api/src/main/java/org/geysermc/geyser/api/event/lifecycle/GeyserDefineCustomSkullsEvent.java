package org.geysermc.geyser.api.event.lifecycle;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Event;

/**
 * Called on Geyser's startup when looking for custom skulls. Custom skulls must be registered through this event.
 * <p>
 * This event will not be called if the "add-non-bedrock-items" setting is disabled in the Geyser config.
 */
public abstract class GeyserDefineCustomSkullsEvent implements Event {
    /**
     * The type of texture provided
     */
    public enum SkullTextureType {
        USERNAME,
        UUID,
        PROFILE,
        SKIN_HASH
    }

    /**
     * Registers the given username, UUID, base64 encoded profile, or skin hash as a custom skull blocks
     * @param texture the username, UUID, base64 encoded profile, or skin hash
     * @param type the type of texture provided
     */
    public abstract void register(@NonNull String texture, @NonNull SkullTextureType type);
}
