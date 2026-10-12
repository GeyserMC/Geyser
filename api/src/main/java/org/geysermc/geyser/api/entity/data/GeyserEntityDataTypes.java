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
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Contains {@link GeyserEntityDataType} types which can be used to change the
 * current value stored for a specific entity data type.
 *
 * @since 2.11.0
 */
@ApiStatus.Experimental
public final class GeyserEntityDataTypes {

    /**
     * Represents a single-byte value used for the main color, ranged 0 to 15.
     *
     * @see <a href="https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/entitycomponents/minecraftcomponent_color?view=minecraft-bedrock-stable">Official documentation</a>
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Byte> COLOR =
        GeyserEntityDataType.create(Identifier.of("color"), Byte.class);

    /**
     * Represents a numeric variant index that can be queried in resource packs.
     *
     * @see <a href="https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/entitycomponents/minecraftcomponent_variant?view=minecraft-bedrock-stable">Official documentation</a>
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Integer> VARIANT =
        GeyserEntityDataType.create(Identifier.of("variant"), Integer.class);

    /**
     * Represents the entity's collision box width.
     *
     * @see <a href="https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/entitycomponents/minecraftcomponent_collision_box?view=minecraft-bedrock-stable">Official documentation</a>
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Float> WIDTH =
        GeyserEntityDataType.create(Identifier.of("geysermc", "width"), Float.class);

    /**
     * Represents the entity's collision box height.
     *
     * @see <a href="https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/entitycomponents/minecraftcomponent_collision_box?view=minecraft-bedrock-stable">Official documentation</a>
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Float> HEIGHT =
        GeyserEntityDataType.create(Identifier.of("geysermc", "height"), Float.class);

    /**
     * Represents the entity's vertical offset applied on top of the Java position sent by the server.
     * Some entities, such as players, boats, and minecarts need such an offset to be displayed in the correct position.
     *
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Float> VERTICAL_OFFSET =
        GeyserEntityDataType.create(Identifier.of("geysermc", "vertical_offset"), Float.class);

    /**
     * Represents the scale multiplier for visual size.
     *
     * @see <a href="https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/entitycomponents/minecraftcomponent_scale?view=minecraft-bedrock-stable">Official documentation"</a>
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Float> SCALE =
        GeyserEntityDataType.create(Identifier.of("scale"), Float.class);

    /**
     * Represents custom hitboxes for entities, or an empty list if none are set.
     * To set no hitbox, update hitboxes to an empty list. To restore default behavior, update it to null.
     *
     * @see <a href="https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/entitycomponents/minecraftcomponent_custom_hit_test?view=minecraft-bedrock-stable">Official documentation</a>
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserListEntityDataType<Hitbox> HITBOXES =
        GeyserListEntityDataType.createList(Identifier.of("custom_hit_test"), Hitbox.class);

    /**
     * Represents the entity's seat offset, which is used when riding another entity.
     * Equivalent to "seats" in the {@code minecraft:rideable} entity component, but set on the rider instead of the vehicle.
     *
     * @see <a href="https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/entitycomponents/minecraftcomponent_rideable?view=minecraft-bedrock-stable">Official documentation</a>
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Vector3f> SEAT_OFFSET =
        GeyserEntityDataType.create(Identifier.of("geysermc", "seat_offset"), Vector3f.class);

    /**
     * Whether the entity's rotation is locked to the vehicle it is riding.
     * This is used for boats and happy ghasts in the vanilla game.
     *
     * @see <a href="https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/entitycomponents/minecraftcomponent_rotation_locked_to_vehicle?view=minecraft-bedrock-stable">Official documentation</a>
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Boolean> ROTATION_LOCKED_TO_VEHICLE =
        GeyserEntityDataType.create(Identifier.of("rotation_locked_to_vehicle"), Boolean.class);

    /**
     * The degrees of rotation a rider may rotate within, set on the rider entity.
     * This is used for boats and happy ghasts in the vanilla game to prevent "looking backwards" while riding it
     *
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Float> SEAT_LOCK_RIDER_ROTATION_DEGREES =
        GeyserEntityDataType.create(Identifier.of("seat_lock_rider_rotation_degrees"), Float.class);

    /**
     * Whether an entity riding another vehicle entity is sitting on a seat which has a rotation.
     *
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Boolean> SEAT_HAS_ROTATION =
        GeyserEntityDataType.create(Identifier.of("geysermc", "seat_has_rotation"), Boolean.class);

    /**
     * The degrees of rotation that this seat is offset by. Equivalent to "rotate_rider_by" in the {@code minecraft:rideable} entity component.
     * This is used for boats to make the player face forward, and is equivalent to {@code rotate_rider_by} in the {@code minecraft:rideable} entity component.
     *
     * @see <a href="https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/entitycomponents/minecraftcomponent_rideable?view=minecraft-bedrock-stable">Official documentation</a>
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public static final GeyserEntityDataType<Float> ROTATE_RIDER_DEGREES =
        GeyserEntityDataType.create(Identifier.of("rotate_rider_by"), Float.class);

    @ApiStatus.Internal
    private GeyserEntityDataTypes() {
        // no-op
    }
}
