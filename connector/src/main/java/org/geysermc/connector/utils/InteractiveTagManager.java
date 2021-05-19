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

import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityDataMap;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import lombok.Getter;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.living.animal.AnimalEntity;
import org.geysermc.connector.entity.living.animal.horse.HorseEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemEntry;

import java.util.EnumSet;
import java.util.Set;

public class InteractiveTagManager {
    /**
     * All entity types that can be leashed on Java Edition
     */
    private static final Set<EntityType> LEASHABLE_MOB_TYPES = EnumSet.of(EntityType.BEE, EntityType.CAT, EntityType.CHICKEN,
            EntityType.COW, EntityType.DOLPHIN, EntityType.DONKEY, EntityType.FOX, EntityType.HOGLIN, EntityType.HORSE, EntityType.SKELETON_HORSE,
            EntityType.ZOMBIE_HORSE, EntityType.IRON_GOLEM, EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.MOOSHROOM,
            EntityType.MULE, EntityType.OCELOT, EntityType.PARROT, EntityType.PIG, EntityType.POLAR_BEAR, EntityType.RABBIT,
            EntityType.SHEEP, EntityType.SNOW_GOLEM, EntityType.STRIDER, EntityType.WOLF, EntityType.ZOGLIN);

    private static final Set<EntityType> SADDLEABLE_WHEN_TAMED_MOB_TYPES = EnumSet.of(EntityType.DONKEY, EntityType.HORSE,
            EntityType.ZOMBIE_HORSE, EntityType.MULE);

    /**
     * Update the suggestion that the client currently has on their screen for this entity (for example, "Feed" or "Ride")
     *
     * @param session the Bedrock client session
     * @param interactEntity the entity that the client is currently facing.
     */
    public static void updateTag(GeyserSession session, Entity interactEntity) {
        EntityDataMap entityMetadata = interactEntity.getMetadata();
        ItemEntry itemEntry = session.getPlayerInventory().getItemInHand().getItemEntry();
        String javaIdentifierStripped = itemEntry.getJavaIdentifier().replace("minecraft:", "");

        InteractiveTag interactiveTag = InteractiveTag.NONE;

        if (entityMetadata.getLong(EntityData.LEASH_HOLDER_EID) == session.getPlayerEntity().getGeyserId()) {
            // Unleash the entity
            interactiveTag = InteractiveTag.REMOVE_LEASH;
        } else if (javaIdentifierStripped.equals("saddle") && !entityMetadata.getFlags().getFlag(EntityFlag.SADDLED) &&
                ((SADDLEABLE_WHEN_TAMED_MOB_TYPES.contains(interactEntity.getEntityType()) && entityMetadata.getFlags().getFlag(EntityFlag.TAMED) && !session.isSneaking()) ||
                        interactEntity.getEntityType() == EntityType.PIG || interactEntity.getEntityType() == EntityType.STRIDER)) {
            // Entity can be saddled and the conditions meet (entity can be saddled and, if needed, is tamed)
            interactiveTag = InteractiveTag.SADDLE;
        } else if (javaIdentifierStripped.equals("name_tag") && session.getPlayerInventory().getItemInHand().getNbt() != null &&
                session.getPlayerInventory().getItemInHand().getNbt().contains("display")) {
            // Holding a named name tag
            interactiveTag = InteractiveTag.NAME;
        } else if (javaIdentifierStripped.equals("lead") && LEASHABLE_MOB_TYPES.contains(interactEntity.getEntityType()) &&
                entityMetadata.getLong(EntityData.LEASH_HOLDER_EID, -1L) == -1L) {
            // Holding a leash and the mob is leashable for sure
            // (Plugins can change this behavior so that's something to look into in the far far future)
            interactiveTag = InteractiveTag.LEASH;
        } else if (interactEntity instanceof AnimalEntity && ((AnimalEntity) interactEntity).canEat(session, javaIdentifierStripped, itemEntry)) {
            // This animal can be fed
            interactiveTag = InteractiveTag.FEED;
        } else {
            switch (interactEntity.getEntityType()) {
                case BOAT:
                    if (interactEntity.getPassengers().size() < 2) {
                        interactiveTag = InteractiveTag.BOARD_BOAT;
                    }
                    break;
                case CAT:
                    if (entityMetadata.getFlags().getFlag(EntityFlag.TAMED) &&
                            entityMetadata.getLong(EntityData.OWNER_EID) == session.getPlayerEntity().getGeyserId()) {
                        // Tamed and owned by player - can sit/stand
                        interactiveTag = entityMetadata.getFlags().getFlag(EntityFlag.SITTING) ? InteractiveTag.STAND : InteractiveTag.SIT;
                        break;
                    }
                    break;
                case MOOSHROOM:
                    // Shear the mooshroom
                    if (javaIdentifierStripped.equals("shears")) {
                        interactiveTag = InteractiveTag.MOOSHROOM_SHEAR;
                        break;
                    }
                    // Bowls are acceptable here
                    else if (javaIdentifierStripped.equals("bowl")) {
                        interactiveTag = InteractiveTag.MOOSHROOM_MILK_STEW;
                        break;
                    }
                    // Fall down to COW as this works on mooshrooms
                case COW:
                    if (javaIdentifierStripped.equals("bucket")) {
                        // Milk the cow
                        interactiveTag = InteractiveTag.MILK;
                    }
                    break;
                case CREEPER:
                    if (javaIdentifierStripped.equals("flint_and_steel")) {
                        // Today I learned that you can ignite a creeper with flint and steel! Huh.
                        interactiveTag = InteractiveTag.IGNITE_CREEPER;
                    }
                    break;
                case DONKEY:
                case LLAMA:
                case MULE:
                    if (entityMetadata.getFlags().getFlag(EntityFlag.TAMED) && !entityMetadata.getFlags().getFlag(EntityFlag.CHESTED)
                            && javaIdentifierStripped.equals("chest")) {
                        // Can attach a chest
                        interactiveTag = InteractiveTag.ATTACH_CHEST;
                        break;
                    }
                    // Intentional fall-through
                case HORSE:
                case SKELETON_HORSE:
                case TRADER_LLAMA:
                case ZOMBIE_HORSE:
                    boolean tamed = entityMetadata.getFlags().getFlag(EntityFlag.TAMED);
                    if (session.isSneaking() && tamed && (interactEntity instanceof HorseEntity || entityMetadata.getFlags().getFlag(EntityFlag.CHESTED))) {
                        interactiveTag = InteractiveTag.OPEN_CONTAINER;
                        break;
                    }
                    if (!entityMetadata.getFlags().getFlag(EntityFlag.BABY)) {
                        // Can't ride a baby
                        if (tamed) {
                            interactiveTag = InteractiveTag.RIDE_HORSE;
                        } else if (itemEntry.getJavaId() == 0) {
                            // Can't hide an untamed entity without having your hand empty
                            interactiveTag = InteractiveTag.MOUNT;
                        }
                    }
                    break;
                case MINECART:
                    if (interactEntity.getPassengers().isEmpty()) {
                        interactiveTag = InteractiveTag.RIDE_MINECART;
                    }
                    break;
                case MINECART_CHEST:
                case MINECART_COMMAND_BLOCK:
                case MINECART_HOPPER:
                    interactiveTag = InteractiveTag.OPEN_CONTAINER;
                    break;
                case PIG:
                    if (entityMetadata.getFlags().getFlag(EntityFlag.SADDLED)) {
                        interactiveTag = InteractiveTag.MOUNT;
                    }
                    break;
                case PIGLIN:
                    if (!entityMetadata.getFlags().getFlag(EntityFlag.BABY) && javaIdentifierStripped.equals("gold_ingot")) {
                        interactiveTag = InteractiveTag.BARTER;
                    }
                    break;
                case SHEEP:
                    if (!entityMetadata.getFlags().getFlag(EntityFlag.SHEARED)) {
                        if (javaIdentifierStripped.equals("shears")) {
                            // Shear the sheep
                            interactiveTag = InteractiveTag.SHEAR;
                        } else if (javaIdentifierStripped.contains("_dye")) {
                            // Dye the sheep
                            interactiveTag = InteractiveTag.DYE;
                        }
                    }
                    break;
                case STRIDER:
                    if (entityMetadata.getFlags().getFlag(EntityFlag.SADDLED)) {
                        interactiveTag = InteractiveTag.RIDE_STRIDER;
                    }
                    break;
                case VILLAGER:
                    if (entityMetadata.getInt(EntityData.VARIANT) != 14 && entityMetadata.getInt(EntityData.VARIANT) != 0
                            && entityMetadata.getFloat(EntityData.SCALE) >= 0.75f) { // Not a nitwit, has a profession and is not a baby
                        interactiveTag = InteractiveTag.TRADE;
                    }
                    break;
                case WANDERING_TRADER:
                    interactiveTag = InteractiveTag.TRADE; // Since you can always trade with a wandering villager, presumably.
                    break;
                case WOLF:
                    if (javaIdentifierStripped.equals("bone") && !entityMetadata.getFlags().getFlag(EntityFlag.TAMED)) {
                        // Bone and untamed - can tame
                        interactiveTag = InteractiveTag.TAME;
                    } else if (entityMetadata.getFlags().getFlag(EntityFlag.TAMED) &&
                            entityMetadata.getLong(EntityData.OWNER_EID) == session.getPlayerEntity().getGeyserId()) {
                        // Tamed and owned by player - can sit/stand
                        interactiveTag = entityMetadata.getFlags().getFlag(EntityFlag.SITTING) ? InteractiveTag.STAND : InteractiveTag.SIT;
                    }
                    break;
                case ZOMBIE_VILLAGER:
                    // We can't guarantee the existence of the weakness effect so we just always show it.
                    if (javaIdentifierStripped.equals("golden_apple")) {
                        interactiveTag = InteractiveTag.CURE;
                    }
                    break;
                default:
                    break;
            }
        }
        session.getPlayerEntity().getMetadata().put(EntityData.INTERACTIVE_TAG, interactiveTag.getValue());
        session.getPlayerEntity().updateBedrockMetadata(session);
    }

    /**
     * All interactive tags in enum form. For potential API usage.
     */
    public enum InteractiveTag {
        NONE(true),
        IGNITE_CREEPER("creeper"),
        EDIT,
        LEAVE_BOAT("exit.boat"),
        FEED,
        FISH("fishing"),
        MILK,
        MOOSHROOM_SHEAR("mooshear"),
        MOOSHROOM_MILK_STEW("moostew"),
        BOARD_BOAT("ride.boat"),
        RIDE_MINECART("ride.minecart"),
        RIDE_HORSE("ride.horse"),
        RIDE_STRIDER("ride.strider"),
        SHEAR,
        SIT,
        STAND,
        TALK,
        TAME,
        DYE,
        CURE,
        OPEN_CONTAINER("opencontainer"),
        CREATE_MAP("createMap"),
        TAKE_PICTURE("takepicture"),
        SADDLE,
        MOUNT,
        BOOST,
        WRITE,
        LEASH,
        REMOVE_LEASH("unleash"),
        NAME,
        ATTACH_CHEST("attachchest"),
        TRADE,
        POSE_ARMOR_STAND("armorstand.pose"),
        EQUIP_ARMOR_STAND("armorstand.equip"),
        READ,
        WAKE_VILLAGER("wakevillager"),
        BARTER;

        /**
         * The full string that should be passed on to the client.
         */
        @Getter
        private final String value;

        InteractiveTag(boolean isNone) {
            this.value = "";
        }

        InteractiveTag(String value) {
            this.value = "action.interact." + value;
        }

        InteractiveTag() {
            this.value = "action.interact." + name().toLowerCase();
        }
    }
}
