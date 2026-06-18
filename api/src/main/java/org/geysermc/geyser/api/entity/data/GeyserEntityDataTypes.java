/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.api.entity.data;

import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.api.entity.data.types.Hitbox;

/**
 * Contains {@link GeyserEntityDataType} types which can be used to change the
 * current value stored for a specific entity data type.
 *
 * TODO: Document
 * https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/entitycomponents/minecraftcomponent_color?view=minecraft-bedrock-stable
 *
 * @since 2.11.0
 */
public final class GeyserEntityDataTypes {

    /**
     * Represents a single-byte value used for color types
     * (e.g., sheep wool color).
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Byte> COLOR =
        GeyserEntityDataType.of(Byte.class, "color");

    /**
     * Represents a numeric variant index that can be queried in resource packs.
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Integer> VARIANT =
        GeyserEntityDataType.of(Integer.class, "variant");

    /**
     * Represents the entity's width.
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Float> WIDTH =
        GeyserEntityDataType.of(Float.class, "width");

    /**
     * Represents the entity's height.
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Float> HEIGHT =
        GeyserEntityDataType.of(Float.class, "height");

    /**
     * Represents the entity's vertical offset.
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Float> VERTICAL_OFFSET =
        GeyserEntityDataType.of(Float.class, "vertical_offset");

    /**
     * Represents the scale multiplier.
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Float> SCALE =
        GeyserEntityDataType.of(Float.class, "scale");

    /**
     * Represents custom hitboxes for entities, or an empty list if none are set.
     *
     * @since 2.11.0
     */
    public static final GeyserListEntityDataType<Hitbox> HITBOXES =
        GeyserListEntityDataType.of(Hitbox.class, "hitboxes");

    /**
     * Represents the entity's seat offset, which is used when riding another entity
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Vector3f> SEAT_OFFSET =
        GeyserEntityDataType.of(Vector3f.class, "seat_offset");

    /**
     * todo: document
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Boolean> SEAT_LOCK_RIDER_ROTATION =
        GeyserEntityDataType.of(Boolean.class, "seat_lock_rider_rotation");

    /**
     * The degrees of rotation the seat is locked to the rider's rotation
     * This is used for boats or happy ghasts in the vanilla game.
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Float> SEAT_LOCK_RIDER_ROTATION_DEGREES =
        GeyserEntityDataType.of(Float.class, "seat_lock_rider_rotation_degrees");

    /**
     * todo: document
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Boolean> SEAT_HAS_ROTATION =
        GeyserEntityDataType.of(Boolean.class, "seat_has_rotation");

    /**
     * todo: document
     *
     * @since 2.11.0
     */
    public static final GeyserEntityDataType<Float> SEAT_ROTATION_OFFSET_DEGREES =
        GeyserEntityDataType.of(Float.class, "seat_rotation_offset_degrees");

    private GeyserEntityDataTypes() {
        // no-op
    }
}
