package org.geysermc.geyser.api.event.lifecycle;

import lombok.NonNull;
import org.geysermc.event.Event;

/**
 * Called on Geyser's startup when looking for custom skulls. Custom skulls must be registered through this event.
 *
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
        SKIN_URL
    }

    /**
     * Registers the given username, UUID, or base64 encoded profile as a custom skull blocks
     * @param texture the username, UUID, or base64 encoded profile
     * @param type the type of texture provided
     */
    public abstract void registerCustomSkull(@NonNull String texture, @NonNull SkullTextureType type);
}
