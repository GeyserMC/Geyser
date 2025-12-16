package org.geysermc.geyser.api.entity.data;

import org.geysermc.geyser.api.entity.data.types.Hitbox;

/**
 * Contains commonly used {@link GeyserEntityDataType} constants for built-in entity
 * metadata fields.
 * <p>
 * These data types define the structure of certain primitive or numeric metadata
 * values that can be used for Bedrock entities. Each constant is backed by a
 * pre-registered entity data type that can be used when reading or writing metadata
 * through the Geyser API.
 */
public final class GeyserEntityDataTypes {

    /**
     * Represents a single-byte value used for color types
     * (e.g., sheep wool color).
     */
    public static final GeyserEntityDataType<Byte> COLOR =
        GeyserEntityDataType.of(Byte.class, "color");

    /**
     * Represents a numeric variant index that can be queried in resource packs.
     */
    public static final GeyserEntityDataType<Integer> VARIANT =
        GeyserEntityDataType.of(Integer.class, "variant");

    /**
     * Represents the entity's width.
     */
    public static final GeyserEntityDataType<Float> WIDTH =
        GeyserEntityDataType.of(Float.class, "width");

    /**
     * Represents the entity's height.
     */
    public static final GeyserEntityDataType<Float> HEIGHT =
        GeyserEntityDataType.of(Float.class, "height");

    /**
     * Represents the entity's vertical offset.
     */
    public static final GeyserEntityDataType<Float> VERTICAL_OFFSET =
        GeyserEntityDataType.of(Float.class, "vertical_offset");

    /**
     * Represents the scale multiplier.
     */
    public static final GeyserEntityDataType<Float> SCALE =
        GeyserEntityDataType.of(Float.class, "scale");

    /**
     * Represents custom hitboxes for entities
     */
    public static final GeyserListEntityDataType<Hitbox> HITBOXES =
        GeyserListEntityDataType.of(Hitbox.class, "hitboxes");

    private GeyserEntityDataTypes() {
        // no-op
    }
}
