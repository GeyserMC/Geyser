/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.util;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.entity.type.living.animal.AnimalEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Locale;

public final class EntityUtils {
    /**
     * A constant array of the two hands that a player can interact with an entity.
     */
    public static final Hand[] HANDS = Hand.values();

    /**
     * @return a new String array of all known effect identifiers
     */
    public static String[] getAllEffectIdentifiers() {
        String[] identifiers = new String[Effect.VALUES.length];
        for (int i = 0; i < Effect.VALUES.length; i++) {
            identifiers[i] = "minecraft:" + Effect.VALUES[i].name().toLowerCase(Locale.ROOT);
        }

        return identifiers;
    }

    /**
     * Convert Java edition effect IDs to Bedrock edition
     *
     * @param effect Effect to convert
     * @return The numeric ID for the Bedrock edition effect
     */
    public static int toBedrockEffectId(Effect effect) {
        return switch (effect) {
            case GLOWING, LUCK, UNLUCK, DOLPHINS_GRACE -> 0; // All Java-exclusive effects as of 1.16.2
            case LEVITATION -> 24;
            case CONDUIT_POWER -> 26;
            case SLOW_FALLING -> 27;
            case BAD_OMEN -> 28;
            case HERO_OF_THE_VILLAGE -> 29;
            case DARKNESS -> 30;
            default -> effect.ordinal() + 1;
        };
    }

    private static float getMountedHeightOffset(Entity mount) {
        float height = mount.getBoundingBoxHeight();
        float mountedHeightOffset = height * 0.75f;
        switch (mount.getDefinition().entityType()) {
            case CHICKEN, SPIDER -> mountedHeightOffset = height * 0.5f;
            case DONKEY, MULE -> mountedHeightOffset -= 0.25f;
            case TRADER_LLAMA, LLAMA -> mountedHeightOffset = height * 0.6f;
            case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                    COMMAND_BLOCK_MINECART -> mountedHeightOffset = 0;
            case BOAT, CHEST_BOAT -> mountedHeightOffset = -0.1f;
            case HOGLIN, ZOGLIN -> {
                boolean isBaby = mount.getFlag(EntityFlag.BABY);
                mountedHeightOffset = height - (isBaby ? 0.2f : 0.15f);
            }
            case PIGLIN -> mountedHeightOffset = height * 0.92f;
            case RAVAGER -> mountedHeightOffset = 2.1f;
            case SKELETON_HORSE -> mountedHeightOffset -= 0.1875f;
            case STRIDER -> mountedHeightOffset = height - 0.19f;
        }
        return mountedHeightOffset;
    }

    private static float getHeightOffset(Entity passenger) {
        boolean isBaby;
        switch (passenger.getDefinition().entityType()) {
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
                isBaby = passenger.getFlag(EntityFlag.BABY);
                return isBaby ? -0.05f : -0.45f;
            case ZOMBIE:
                isBaby = passenger.getFlag(EntityFlag.BABY);
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
    public static void updateMountOffset(Entity passenger, Entity mount, boolean rider, boolean riding, boolean moreThanOneEntity) {
        passenger.setFlag(EntityFlag.RIDING, riding);
        if (riding) {
            // Without the Y offset, Bedrock players will find themselves in the floor when mounting
            float mountedHeightOffset = getMountedHeightOffset(mount);
            float heightOffset = getHeightOffset(passenger);

            float xOffset = 0;
            float yOffset = mountedHeightOffset + heightOffset;
            float zOffset = 0;
            switch (mount.getDefinition().entityType()) {
                case BOAT -> {
                    // Without the X offset, more than one entity on a boat is stacked on top of each other
                    if (rider && moreThanOneEntity) {
                        xOffset = 0.2f;
                    } else if (moreThanOneEntity) {
                        xOffset = -0.6f;
                    }
                }
                case CHEST_BOAT -> xOffset = 0.15F;
                case CHICKEN -> zOffset = -0.1f;
                case TRADER_LLAMA, LLAMA -> zOffset = -0.3f;
            }
            if (passenger.getDefinition().entityType() == EntityType.SHULKER) {
                switch (mount.getDefinition().entityType()) {
                    case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                            COMMAND_BLOCK_MINECART, BOAT, CHEST_BOAT -> yOffset = 0.1875f;
                }
            }
            /*
             * Bedrock Differences
             * Zoglin & Hoglin seem to be taller in Bedrock edition
             * Horses are tinier
             * Players, Minecarts, and Boats have different origins
             */
            if (passenger.getDefinition().entityType() == EntityType.PLAYER) {
                if (mount.getDefinition().entityType() != EntityType.PLAYER && mount.getDefinition().entityType() != EntityType.AREA_EFFECT_CLOUD) {
                    yOffset += EntityDefinitions.PLAYER.offset();
                }
            }
            switch (mount.getDefinition().entityType()) {
                case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                        COMMAND_BLOCK_MINECART, BOAT, CHEST_BOAT -> yOffset -= mount.getDefinition().height() * 0.5f;
            }
            if (passenger.getDefinition().entityType() == EntityType.FALLING_BLOCK) {
                yOffset += 0.5f;
            }
            if (mount instanceof ArmorStandEntity armorStand) {
                yOffset -= armorStand.getYOffset();
            }
            Vector3f offset = Vector3f.from(xOffset, yOffset, zOffset);
            passenger.setRiderSeatPosition(offset);
        }
    }

    public static void updateRiderRotationLock(Entity passenger, Entity mount, boolean isRiding) {
        if (isRiding && mount instanceof BoatEntity) {
            // Head rotation is locked while riding in a boat
            passenger.getDirtyMetadata().put(EntityData.RIDER_ROTATION_LOCKED, (byte) 1);
            passenger.getDirtyMetadata().put(EntityData.RIDER_MAX_ROTATION, 90f);
            passenger.getDirtyMetadata().put(EntityData.RIDER_MIN_ROTATION, 1f);
            passenger.getDirtyMetadata().put(EntityData.RIDER_ROTATION_OFFSET, -90f);
        } else {
            passenger.getDirtyMetadata().put(EntityData.RIDER_ROTATION_LOCKED, (byte) 0);
            passenger.getDirtyMetadata().put(EntityData.RIDER_MAX_ROTATION, 0f);
            passenger.getDirtyMetadata().put(EntityData.RIDER_MIN_ROTATION, 0f);
            passenger.getDirtyMetadata().put(EntityData.RIDER_ROTATION_OFFSET, 0f);
        }
    }

    /**
     * Determine if an action would result in a successful bucketing of the given entity.
     */
    public static boolean attemptToBucket(GeyserSession session, GeyserItemStack itemInHand) {
        return itemInHand.getJavaId() == session.getItemMappings().getStoredItems().waterBucket();
    }

    /**
     * Attempt to determine the result of saddling the given entity.
     */
    public static InteractionResult attemptToSaddle(GeyserSession session, Entity entityToSaddle, GeyserItemStack itemInHand) {
        if (itemInHand.getJavaId() == session.getItemMappings().getStoredItems().saddle()) {
            if (!entityToSaddle.getFlag(EntityFlag.SADDLED) && !entityToSaddle.getFlag(EntityFlag.BABY)) {
                // Saddle
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private EntityUtils() {
    }
}
