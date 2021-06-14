/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.living.ArmorStandEntity;
import org.geysermc.connector.entity.living.animal.AnimalEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public final class EntityUtils {

    /**
     * Convert Java edition effect IDs to Bedrock edition
     *
     * @param effect Effect to convert
     * @return The numeric ID for the Bedrock edition effect
     */
    public static int toBedrockEffectId(Effect effect) {
        switch (effect) {
            case GLOWING:
            case LUCK:
            case UNLUCK:
            case DOLPHINS_GRACE:
                // All Java-exclusive effects as of 1.16.2
                return 0;
            case LEVITATION:
                return 24;
            case CONDUIT_POWER:
                return 26;
            case SLOW_FALLING:
                return 27;
            case BAD_OMEN:
                return 28;
            case HERO_OF_THE_VILLAGE:
                return 29;
            default:
                return effect.ordinal() + 1;
        }
    }

    /**
     * Converts a MobType to a Bedrock edition EntityType, returns null if the EntityType is not found
     *
     * @param type The MobType to convert
     * @return Converted EntityType
     */
    public static EntityType toBedrockEntity(com.github.steveice10.mc.protocol.data.game.entity.type.EntityType type) {
        try {
            return EntityType.valueOf(type.name());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static float getMountedHeightOffset(Entity mount) {
        float height = mount.getMetadata().getFloat(EntityData.BOUNDING_BOX_HEIGHT);
        float mountedHeightOffset = height * 0.75f;
        switch (mount.getEntityType()) {
            case CHICKEN:
            case SPIDER:
                mountedHeightOffset = height * 0.5f;
                break;
            case DONKEY:
            case MULE:
                mountedHeightOffset -= 0.25f;
                break;
            case LLAMA:
                mountedHeightOffset = height * 0.67f;
                break;
            case MINECART:
            case MINECART_HOPPER:
            case MINECART_TNT:
            case MINECART_CHEST:
            case MINECART_FURNACE:
            case MINECART_SPAWNER:
            case MINECART_COMMAND_BLOCK:
                mountedHeightOffset = 0;
                break;
            case BOAT:
                mountedHeightOffset = -0.1f;
                break;
            case HOGLIN:
            case ZOGLIN:
                boolean isBaby = mount.getMetadata().getFlags().getFlag(EntityFlag.BABY);
                mountedHeightOffset = height - (isBaby ? 0.2f : 0.15f);
                break;
            case PIGLIN:
                mountedHeightOffset = height * 0.92f;
                break;
            case RAVAGER:
                mountedHeightOffset = 2.1f;
                break;
            case SKELETON_HORSE:
                mountedHeightOffset -= 0.1875f;
                break;
            case STRIDER:
                mountedHeightOffset = height - 0.19f;
                break;
        }
        return mountedHeightOffset;
    }

    private static float getHeightOffset(Entity passenger) {
        boolean isBaby;
        switch (passenger.getEntityType()) {
            case SKELETON:
            case STRAY:
            case WITHER_SKELETON:
                return -0.6f;
            case ARMOR_STAND:
                if (((ArmorStandEntity) passenger).isMarker()) {
                    return 0.0f;
                } else {
                    return 0.1f;
                }
            case ENDERMITE:
            case SILVERFISH:
                return 0.1f;
            case PIGLIN:
            case PIGLIN_BRUTE:
            case ZOMBIFIED_PIGLIN:
                isBaby = passenger.getMetadata().getFlags().getFlag(EntityFlag.BABY);
                return isBaby ? -0.05f : -0.45f;
            case ZOMBIE:
                isBaby = passenger.getMetadata().getFlags().getFlag(EntityFlag.BABY);
                return isBaby ? 0.0f : -0.45f;
            case EVOKER:
            case ILLUSIONER:
            case PILLAGER:
            case RAVAGER:
            case VINDICATOR:
            case WITCH:
                return -0.45f;
            case PLAYER:
                return -0.35f;
        }
        if (passenger instanceof AnimalEntity) {
            return 0.14f;
        }
        return 0f;
    }

    /**
     * Adjust an entity's height if they have mounted/dismounted an entity.
     */
    public static void updateMountOffset(Entity passenger, Entity mount, GeyserSession session, boolean rider, boolean riding, boolean moreThanOneEntity) {
        passenger.getMetadata().getFlags().setFlag(EntityFlag.RIDING, riding);
        if (riding) {
            // Without the Y offset, Bedrock players will find themselves in the floor when mounting
            float mountedHeightOffset = getMountedHeightOffset(mount);
            float heightOffset = getHeightOffset(passenger);

            float xOffset = 0;
            float yOffset = mountedHeightOffset + heightOffset;
            float zOffset = 0;
            switch (mount.getEntityType()) {
                case BOAT:
                    // Without the X offset, more than one entity on a boat is stacked on top of each other
                    if (rider && moreThanOneEntity) {
                        xOffset = 0.2f;
                    } else if (moreThanOneEntity) {
                        xOffset = -0.6f;
                    }
                    break;
                case CHICKEN:
                    zOffset = -0.1f;
                    break;
                case LLAMA:
                    zOffset = -0.3f;
                    break;
            }
            /*
             * Bedrock Differences
             * Zoglin & Hoglin seem to be taller in Bedrock edition
             * Horses are tinier
             * Players, Minecarts, and Boats have different origins
             */
            if (passenger.getEntityType() == EntityType.PLAYER && mount.getEntityType() != EntityType.PLAYER) {
                yOffset += EntityType.PLAYER.getOffset();
            }
            switch (mount.getEntityType()) {
                case MINECART:
                case MINECART_HOPPER:
                case MINECART_TNT:
                case MINECART_CHEST:
                case MINECART_FURNACE:
                case MINECART_SPAWNER:
                case MINECART_COMMAND_BLOCK:
                case BOAT:
                    yOffset -= mount.getEntityType().getHeight() * 0.5f;
            }
            Vector3f offset = Vector3f.from(xOffset, yOffset, zOffset);
            passenger.getMetadata().put(EntityData.RIDER_SEAT_POSITION, offset);
        }
        passenger.updateBedrockMetadata(session);
    }
}
