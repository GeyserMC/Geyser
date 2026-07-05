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

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserFloatEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserStringEnumProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.CustomBedrockEntityDefinition;
import org.geysermc.geyser.entity.GeyserEntityType;
import org.geysermc.geyser.entity.VanillaEntities;
import org.geysermc.geyser.entity.properties.type.BooleanProperty;
import org.geysermc.geyser.entity.properties.type.EnumProperty;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.properties.type.PropertyType;
import org.geysermc.geyser.entity.properties.type.StringEnumProperty;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.ChestBoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.TextDisplayEntity;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.entity.type.living.animal.AnimalEntity;
import org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class EntityUtils {

    private static final AtomicInteger RUNTIME_ID_ALLOCATOR = new AtomicInteger(100000);

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

    private static float getMountedHeightOffset(Entity mount) {
        if (mount instanceof BoatEntity boat && boat.getVariant() != BoatEntity.BoatVariant.BAMBOO) {
            return -0.1f;
        }

        float height = mount.getBoundingBoxHeight();
        float mountedHeightOffset = height * 0.75f;
        switch (mount.getEntityType()) {
            case CAMEL -> {
                boolean isBaby = mount.getFlag(EntityFlag.BABY);
                mountedHeightOffset = height - (isBaby ? 0.35f : 0.6f);
            }
            case CAVE_SPIDER, CHICKEN, SPIDER -> mountedHeightOffset = height * 0.5f;
            case DONKEY, MULE -> mountedHeightOffset -= 0.25f;
            case TRADER_LLAMA, LLAMA -> mountedHeightOffset = height * 0.6f;
            case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                    COMMAND_BLOCK_MINECART -> mountedHeightOffset = 0;
            case BAMBOO_RAFT, BAMBOO_CHEST_RAFT -> mountedHeightOffset = 0.25f;
            case HOGLIN, ZOGLIN -> {
                boolean isBaby = mount.getFlag(EntityFlag.BABY);
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
        boolean isBaby;
        switch (passenger.getEntityType()) {
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
                if (vehicle instanceof BoatEntity || vehicle.getJavaDefinition() == VanillaEntities.MINECART) {
                    return 0.1875f - getMountedHeightOffset(vehicle);
                }
        }
        if (passenger instanceof AnimalEntity) {
            return 0.14f;
        }
        return 0f;
    }

    /**
     * Adjust an entity's height if they have mounted/dismounted an entity.
     */
    public static void updateMountOffset(Entity passenger, Entity mount, boolean rider, boolean riding, int index, int passengers) {
        passenger.setFlag(EntityFlag.RIDING, riding);
        if (riding) {
            // Without the Y offset, Bedrock players will find themselves in the floor when mounting
            float mountedHeightOffset = getMountedHeightOffset(mount);
            float heightOffset = getHeightOffset(passenger);

            float xOffset = 0;
            float yOffset = mountedHeightOffset + heightOffset;
            float zOffset = 0;
            switch (mount.getEntityType()) {
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
                // Without the X offset, more than one entity on a boat is stacked on top of each other
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
            if (mount.getJavaDefinition() == VanillaEntities.PLAYER) {
                yOffset -= VanillaEntities.PLAYER.offset();
            }
            if (passenger.getJavaDefinition() == VanillaEntities.PLAYER) {
                yOffset += VanillaEntities.PLAYER.offset();
            }
            switch (mount.getEntityType()) {
                case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                        COMMAND_BLOCK_MINECART -> yOffset -= mount.getJavaDefinition().height() * 0.5f;
            }
            switch (passenger.getEntityType()) {
                case MINECART, HOPPER_MINECART, TNT_MINECART, CHEST_MINECART, FURNACE_MINECART, SPAWNER_MINECART,
                     COMMAND_BLOCK_MINECART, SHULKER -> yOffset += passenger.getJavaDefinition().height() * 0.5f;
                case FALLING_BLOCK -> yOffset += 0.995f;
            }
            if (mount instanceof BoatEntity) {
                yOffset -= mount.getJavaDefinition().height() * 0.5f;
            }
            if (passenger instanceof BoatEntity) {
                yOffset += passenger.getJavaDefinition().height() * 0.5f;
            }
            if (mount instanceof ArmorStandEntity armorStand) {
                yOffset -= armorStand.getYOffset();
            }
            passenger.setRiderSeatPosition(Vector3f.from(xOffset, yOffset, zOffset));
        }
    }

    public static void updateRiderRotationLock(Entity passenger, Entity mount, boolean isRiding) {
        if (isRiding && mount instanceof BoatEntity) {
            // Head rotation is locked while riding in a boat
            passenger.getMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION, true);
            passenger.getMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES, 90f);
            passenger.getMetadata().put(EntityDataTypes.SEAT_HAS_ROTATION, true);
            passenger.getMetadata().put(EntityDataTypes.SEAT_ROTATION_OFFSET_DEGREES, -90f);
        } else {
            passenger.getMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION, false);
            passenger.getMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES, 0f);
            passenger.getMetadata().put(EntityDataTypes.SEAT_HAS_ROTATION, false);
            passenger.getMetadata().put(EntityDataTypes.SEAT_ROTATION_OFFSET_DEGREES, 0f);
        }
    }

    /**
     * Determine if an action would result in a successful bucketing of the given entity.
     */
    public static boolean attemptToBucket(GeyserItemStack itemInHand) {
        return itemInHand.is(Items.WATER_BUCKET);
    }

    /**
     * Attempt to determine the result of saddling the given entity.
     */
    public static InteractionResult attemptToSaddle(Entity entityToSaddle, GeyserItemStack itemInHand) {
        if (itemInHand.is(Items.SADDLE)) {
            if (!entityToSaddle.getFlag(EntityFlag.SADDLED) && !entityToSaddle.getFlag(EntityFlag.BABY)) {
                // Saddle
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * Convert Java GameMode to Bedrock GameType
     * Needed to account for ordinal differences (spectator is 3 in Java, 6 in Bedrock)
     */
    @SuppressWarnings("deprecation") // Must use survival_viewer due to limitations on Bedrock's spectator gamemode
    public static GameType toBedrockGamemode(GameMode gamemode) {
        return switch (gamemode) {
            case CREATIVE -> GameType.CREATIVE;
            case ADVENTURE -> GameType.ADVENTURE;
            case SPECTATOR -> GameType.SURVIVAL_VIEWER;
            default -> GameType.SURVIVAL;
        };
    }

    private static String translatedEntityName(@NonNull String namespace, @NonNull String name, @NonNull GeyserSession session) {
        // MinecraftLocale would otherwise invoke getBootstrap (which doesn't exist) and create some folders,
        // so use the default fallback value as used in Minecraft Java
        if (EnvironmentUtils.IS_UNIT_TESTING) {
            return "entity." + namespace + "." + name;
        }
        return MinecraftLocale.getLocaleString("entity." + namespace + "." + name, session.locale());
    }

    public static String translatedEntityName(@NonNull Key type, @NonNull GeyserSession session) {
        return translatedEntityName(type.namespace(), type.value(), session);
    }

    public static String translatedEntityName(@Nullable GeyserEntityType type, @NonNull GeyserSession session) {
        // default fallback value as used in Minecraft Java
        if (type == null || type.isUnregistered()) {
            return "entity.unregistered_sadface";
        } else if (type.is(EntityType.PLAYER)) {
            return "Player"; // the player's name is always shown instead
        }
        // this works at least with all 1.20.5 entities, except the killer bunny since that's not an entity type.
        Identifier typeName = type.identifier();
        return translatedEntityName(typeName.namespace(), typeName.path(), session);
    }

    public static boolean equipmentUsableByEntity(GeyserSession session, Equippable equippable, GeyserEntityType entity) {
        if (equippable.allowedEntities() == null) {
            return true;
        }

        GeyserHolderSet<GeyserEntityType> holderSet = GeyserHolderSet.fromHolderSet(JavaRegistries.ENTITY_TYPE, equippable.allowedEntities());
        return holderSet.contains(session, entity);
    }

    // From ViaVersion! thank u!!
    public static UUID uuidFromIntArray(int[] uuid) {
        if (uuid != null && uuid.length == 4) {
            // thank u viaversion
            return new UUID((long) uuid[0] << 32 | ((long) uuid[1] & 0xFFFFFFFFL),
                (long) uuid[2] << 32 | ((long) uuid[3] & 0xFFFFFFFFL));
        }
        return null;
    }

    public static void callEntityEvents() {
        // entities would be initialized before these events are called
        List<CustomBedrockEntityDefinition> customEntities = new ArrayList<>();
        GeyserImpl.getInstance().getEventBus().fire(new GeyserDefineEntitiesEvent() {

            @Override
            public @NonNull Collection<GeyserEntityDefinition> entities() {
                return Collections.unmodifiableCollection(Registries.BEDROCK_ENTITY_DEFINITIONS.get().values());
            }

            @Override
            public @NonNull Collection<CustomEntityDefinition> customEntities() {
                return Collections.unmodifiableCollection(customEntities);
            }

            @Override
            public void register(@NonNull CustomEntityDefinition entityDefinition) {
                Objects.requireNonNull(entityDefinition);
                if (!(entityDefinition instanceof CustomBedrockEntityDefinition bedrockEntityDefinition)) {
                    throw new IllegalArgumentException("Only CustomBedrockEntityDefinition instances may be registered. Use CustomEntityDefinition.of() to create one. Found: " + entityDefinition.getClass().getSimpleName());
                }
                if (entityDefinition.registered()) {
                    throw new IllegalStateException("Duplicate custom entity definition: " + entityDefinition.identifier());
                }
                Registries.BEDROCK_ENTITY_DEFINITIONS.register(bedrockEntityDefinition.identifier(), bedrockEntityDefinition);
                customEntities.add(bedrockEntityDefinition);
            }
        });

        if (!customEntities.isEmpty()) {
            NbtMap nbt = Registries.BEDROCK_ENTITY_IDENTIFIERS.get();
            List<NbtMap> idlist = new ArrayList<>(nbt.getList("idlist", NbtType.COMPOUND));

            for (BedrockEntityDefinition definition : customEntities) {
                idlist.add(NbtMap.builder()
                    .putBoolean("hasSpawnEgg", false)
                    .putString("id", definition.identifier().toString())
                    .putBoolean("summonable", true)
                    .putString("bid", "")
                    .putInt("rid", RUNTIME_ID_ALLOCATOR.getAndIncrement())
                    .putBoolean("experimental", false)
                    .build());
                GeyserImpl.getInstance().getLogger().debug("Registered custom entity " + definition.identifier());
            }

            NbtMap newIdentifiers = nbt.toBuilder()
                .putList("idlist", NbtType.COMPOUND, idlist)
                .build();

            Registries.BEDROCK_ENTITY_IDENTIFIERS.set(newIdentifiers);
            if (!customEntities.isEmpty()) {
                GeyserImpl.getInstance().getLogger().info("Registered " + customEntities.size() + " custom entities");
            }
        }

        GeyserImpl.getInstance().getEventBus().fire(new GeyserDefineEntityPropertiesEvent() {
            @Override
            public @NonNull GeyserFloatEntityProperty registerFloatProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, float min, float max, @Nullable Float defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                FloatProperty property = new FloatProperty(propertyId, max, min, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            @Override
            public @NonNull IntProperty registerIntegerProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, int min, int max, @Nullable Integer defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                IntProperty property = new IntProperty(propertyId, max, min, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            @Override
            public @NonNull BooleanProperty registerBooleanProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, boolean defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                BooleanProperty property = new BooleanProperty(propertyId, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            @Override
            public <E extends Enum<E>> @NonNull EnumProperty<E> registerEnumProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, @NonNull Class<E> enumClass, @Nullable E defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                Objects.requireNonNull(enumClass);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                EnumProperty<E> property = new EnumProperty<>(propertyId, enumClass, defaultValue == null ? enumClass.getEnumConstants()[0] : defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            @Override
            public @NonNull GeyserStringEnumProperty registerEnumProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, @NonNull List<String> values, @Nullable String defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                Objects.requireNonNull(values);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                StringEnumProperty property = new StringEnumProperty(propertyId, values, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            @Override
            public @NonNull Collection<GeyserEntityProperty<?>> properties(@NonNull Identifier identifier) {
                Objects.requireNonNull(identifier);
                var definition = Registries.BEDROCK_ENTITY_DEFINITIONS.get(identifier);
                if (definition == null) {
                    throw new IllegalArgumentException("Unknown entity type: " + identifier);
                }
                return List.copyOf(definition.registeredProperties().getProperties());
            }
        });

        for (var definition : Registries.BEDROCK_ENTITY_DEFINITIONS.get().values()) {
            if (!definition.registeredProperties().isEmpty()) {
                Registries.BEDROCK_ENTITY_PROPERTIES.get().add(definition.registeredProperties().toNbtMap(definition.identifier().toString()));
            }
        }
    }

    private static <T> void registerProperty(Identifier entityType, PropertyType<T, ?> property) {
        var definition = Registries.BEDROCK_ENTITY_DEFINITIONS.get(entityType);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }

        definition.registeredProperties().add(entityType.toString(), property);
    }

    private EntityUtils() {
    }
}
