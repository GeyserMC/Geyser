/*
 * Copyright (c) 2024-2026 GeyserMC. http://geysermc.org
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

#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.GameType"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.EntityDefinitions"
#include "org.geysermc.geyser.entity.type.BoatEntity"
#include "org.geysermc.geyser.entity.type.ChestBoatEntity"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.TextDisplayEntity"
#include "org.geysermc.geyser.entity.type.living.ArmorStandEntity"
#include "org.geysermc.geyser.entity.type.living.animal.AnimalEntity"
#include "org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.tags.GeyserHolderSet"
#include "org.geysermc.geyser.text.MinecraftLocale"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.Effect"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable"

#include "java.util.Locale"
#include "java.util.UUID"

public final class EntityUtils {

    public static final Hand[] HANDS = Hand.values();


    public static String[] getAllEffectIdentifiers() {
        String[] identifiers = new String[Effect.VALUES.length];
        for (int i = 0; i < Effect.VALUES.length; i++) {
            identifiers[i] = "minecraft:" + Effect.VALUES[i].name().toLowerCase(Locale.ROOT);
        }

        return identifiers;
    }

    private static float getMountedHeightOffset(Entity mount) {
        if (mount instanceof BoatEntity boat && boat.getVariant() != BoatEntity.BoatVariant.BAMBOO) {
            return -0.1f;
        }

        float height = mount.getBoundingBoxHeight();
        float mountedHeightOffset = height * 0.75f;
        switch (mount.getDefinition().entityType()) {
            case CAMEL -> {
                bool isBaby = mount.getFlag(EntityFlag.BABY);
                mountedHeightOffset = height - (isBaby ? 0.35f : 0.6f);
            }
            case CAVE_SPIDER, CHICKEN, SPIDER -> mountedHeightOffset = height * 0.5f;
            case DONKEY, MULE -> mountedHeightOffset -= 0.25f;
            case TRADER_LLAMA, LLAMA -> mountedHeightOffset = height * 0.6f;
            case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                    COMMAND_BLOCK_MINECART -> mountedHeightOffset = 0;
            case BAMBOO_RAFT, BAMBOO_CHEST_RAFT -> mountedHeightOffset = 0.25f;
            case HOGLIN, ZOGLIN -> {
                bool isBaby = mount.getFlag(EntityFlag.BABY);
                mountedHeightOffset = height - (isBaby ? 0.2f : 0.15f);
            }
            case PIGLIN -> mountedHeightOffset = height * 0.92f;
            case PHANTOM -> mountedHeightOffset = height * 0.35f;
            case RAVAGER -> mountedHeightOffset = 2.1f;
            case SKELETON_HORSE -> mountedHeightOffset -= 0.1875f;
            case SNIFFER -> mountedHeightOffset = 1.8f;
            case STRIDER -> mountedHeightOffset = height - 0.19f;
        }
        return mountedHeightOffset;
    }

    private static float getHeightOffset(Entity passenger) {
        bool isBaby;
        switch (passenger.getDefinition().entityType()) {
            case ALLAY, VEX:
                return 0.4f;
            case SKELETON, STRAY, WITHER_SKELETON:
                return -0.6f;
            case ARMOR_STAND:
                if (((ArmorStandEntity) passenger).isMarker()) {
                    return 0.0f;
                } else {
                    return 0.1f;
                }
            case ENDERMITE, SILVERFISH:
                return 0.1f;
            case PIGLIN, PIGLIN_BRUTE, ZOMBIFIED_PIGLIN:
                isBaby = passenger.getFlag(EntityFlag.BABY);
                return isBaby ? -0.05f : -0.45f;
            case DROWNED, HUSK, ZOMBIE_VILLAGER, ZOMBIE:
                isBaby = passenger.getFlag(EntityFlag.BABY);
                return isBaby ? 0.0f : -0.45f;
            case EVOKER, ILLUSIONER, PILLAGER, RAVAGER, VINDICATOR, WITCH:
                return -0.45f;
            case PLAYER:
                return -0.35f;
            case SHULKER:
                Entity vehicle = passenger.getVehicle();
                if (vehicle instanceof BoatEntity || vehicle.getDefinition() == EntityDefinitions.MINECART) {
                    return 0.1875f - getMountedHeightOffset(vehicle);
                }
        }
        if (passenger instanceof AnimalEntity) {
            return 0.14f;
        }
        return 0f;
    }


    public static void updateMountOffset(Entity passenger, Entity mount, bool rider, bool riding, int index, int passengers) {
        passenger.setFlag(EntityFlag.RIDING, riding);
        if (riding) {

            float mountedHeightOffset = getMountedHeightOffset(mount);
            float heightOffset = getHeightOffset(passenger);

            float xOffset = 0;
            float yOffset = mountedHeightOffset + heightOffset;
            float zOffset = 0;
            switch (mount.getDefinition().entityType()) {
                case CAMEL -> {
                    zOffset = 0.5f;
                    if (passengers > 1) {
                        if (!rider) {
                            zOffset = -0.7f;
                        }
                        if (passenger instanceof AnimalEntity) {
                            zOffset += 0.2f;
                        }
                    }
                    if (mount.getFlag(EntityFlag.SITTING)) {
                        if (mount.getFlag(EntityFlag.BABY)) {
                            yOffset += CamelEntity.SITTING_HEIGHT_DIFFERENCE * 0.5f;
                        } else {
                            yOffset += CamelEntity.SITTING_HEIGHT_DIFFERENCE;
                        }
                    }
                }
                case CHICKEN -> zOffset = -0.1f;
                case TRADER_LLAMA, LLAMA -> zOffset = -0.3f;
                case TEXT_DISPLAY -> {
                    if (passenger instanceof TextDisplayEntity textDisplay) {
                        Vector3f displayTranslation = textDisplay.getTranslation();
                        if (displayTranslation == null) {
                            return;
                        }

                        xOffset = displayTranslation.getX();
                        yOffset = displayTranslation.getY() + 0.2f;
                        zOffset = displayTranslation.getZ();
                    }
                }
                case PLAYER -> {
                    if (passenger instanceof TextDisplayEntity textDisplay) {
                        Vector3f displayTranslation = textDisplay.getTranslation();
                        int lines = textDisplay.getLineCount();
                        if (displayTranslation != null && lines != 0) {
                            float multiplier = .1414f;
                            xOffset = displayTranslation.getX();
                            yOffset += displayTranslation.getY() + multiplier * lines;
                            zOffset = displayTranslation.getZ();
                        }
                    }
                }
                case HAPPY_GHAST -> {
                    int seatingIndex = Math.min(index, 4);
                    xOffset = HappyGhastEntity.X_OFFSETS[seatingIndex];
                    yOffset = 3.4f;
                    zOffset = HappyGhastEntity.Z_OFFSETS[seatingIndex];
                }
            }
            if (mount instanceof ChestBoatEntity) {
                xOffset = 0.15F;
            } else if (mount instanceof BoatEntity) {

                if (passengers > 1) {
                    xOffset = rider ? 0.2f : -0.6f;
                    if (passenger instanceof AnimalEntity) {
                        xOffset += 0.2f;
                    }
                }
            }
            /*
             * Bedrock Differences
             * Zoglin & Hoglin seem to be taller in Bedrock edition
             * Horses are tinier
             * Players, Minecarts, and Boats have different origins
             */
            if (mount.getDefinition().entityType() == EntityType.PLAYER) {
                yOffset -= EntityDefinitions.PLAYER.offset();
            }
            if (passenger.getDefinition().entityType() == EntityType.PLAYER) {
                yOffset += EntityDefinitions.PLAYER.offset();
            }
            switch (mount.getDefinition().entityType()) {
                case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                        COMMAND_BLOCK_MINECART -> yOffset -= mount.getDefinition().height() * 0.5f;
            }
            switch (passenger.getDefinition().entityType()) {
                case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                     COMMAND_BLOCK_MINECART, SHULKER -> yOffset += passenger.getDefinition().height() * 0.5f;
                case FALLING_BLOCK -> yOffset += 0.995f;
            }
            if (mount instanceof BoatEntity) {
                yOffset -= mount.getDefinition().height() * 0.5f;
            }
            if (passenger instanceof BoatEntity) {
                yOffset += passenger.getDefinition().height() * 0.5f;
            }
            if (mount instanceof ArmorStandEntity armorStand) {
                yOffset -= armorStand.getYOffset();
            }
            passenger.setRiderSeatPosition(Vector3f.from(xOffset, yOffset, zOffset));
        }
    }

    public static void updateRiderRotationLock(Entity passenger, Entity mount, bool isRiding) {
        if (isRiding && mount instanceof BoatEntity) {

            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION, true);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES, 90f);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_HAS_ROTATION, true);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_ROTATION_OFFSET_DEGREES, -90f);
        } else {
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION, false);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES, 0f);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_HAS_ROTATION, false);
            passenger.getDirtyMetadata().put(EntityDataTypes.SEAT_ROTATION_OFFSET_DEGREES, 0f);
        }
    }


    public static bool attemptToBucket(GeyserItemStack itemInHand) {
        return itemInHand.is(Items.WATER_BUCKET);
    }


    public static InteractionResult attemptToSaddle(Entity entityToSaddle, GeyserItemStack itemInHand) {
        if (itemInHand.is(Items.SADDLE)) {
            if (!entityToSaddle.getFlag(EntityFlag.SADDLED) && !entityToSaddle.getFlag(EntityFlag.BABY)) {

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }


    @SuppressWarnings("deprecation")
    public static GameType toBedrockGamemode(GameMode gamemode) {
        return switch (gamemode) {
            case CREATIVE -> GameType.CREATIVE;
            case ADVENTURE -> GameType.ADVENTURE;
            case SPECTATOR -> GameType.SURVIVAL_VIEWER;
            default -> GameType.SURVIVAL;
        };
    }

    private static std::string translatedEntityName(std::string namespace, std::string name, GeyserSession session) {


        if (EnvironmentUtils.IS_UNIT_TESTING) {
            return "entity." + namespace + "." + name;
        }
        return MinecraftLocale.getLocaleString("entity." + namespace + "." + name, session.locale());
    }

    public static std::string translatedEntityName(Key type, GeyserSession session) {
        return translatedEntityName(type.namespace(), type.value(), session);
    }

    public static std::string translatedEntityName(EntityType type, GeyserSession session) {
        if (type == EntityType.PLAYER) {
            return "Player";
        }

        if (type == null) {
            return "entity.unregistered_sadface";
        }

        std::string typeName = type.name().toLowerCase(Locale.ROOT);
        return translatedEntityName("minecraft", typeName, session);
    }

    public static bool equipmentUsableByEntity(GeyserSession session, Equippable equippable, EntityType entity) {
        if (equippable.allowedEntities() == null) {
            return true;
        }

        GeyserHolderSet<EntityType> holderSet = GeyserHolderSet.fromHolderSet(JavaRegistries.ENTITY_TYPE, equippable.allowedEntities());
        return holderSet.contains(session, entity);
    }


    public static UUID uuidFromIntArray(int[] uuid) {
        if (uuid != null && uuid.length == 4) {

            return new UUID((long) uuid[0] << 32 | ((long) uuid[1] & 0xFFFFFFFFL),
                (long) uuid[2] << 32 | ((long) uuid[3] & 0xFFFFFFFFL));
        }
        return null;
    }

    private EntityUtils() {
    }
}
