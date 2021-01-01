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

package org.geysermc.connector.network.translators.bedrock.entity.player;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerStatePacket;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityDataMap;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.InteractPacket;
import lombok.Getter;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;

import java.util.Arrays;
import java.util.List;

@Translator(packet = InteractPacket.class)
public class BedrockInteractTranslator extends PacketTranslator<InteractPacket> {

    /**
     * A list of all foods a horse/donkey can eat on Java Edition.
     * Used to display interactive tag if needed.
     */
    private static final List<String> DONKEY_AND_HORSE_FOODS = Arrays.asList("golden_apple", "enchanted_golden_apple",
            "golden_carrot", "sugar", "apple", "wheat", "hay_block");

    /**
     * A list of all flowers. Used for feeding bees.
     */
    private static final List<String> FLOWERS = Arrays.asList("dandelion", "poppy", "blue_orchid", "allium", "azure_bluet",
            "red_tulip", "pink_tulip", "white_tulip", "orange_tulip", "cornflower", "lily_of_the_valley", "wither_rose",
            "sunflower", "lilac", "rose_bush", "peony");

    /**
     * All entity types that can be leashed on Java Edition
     */
    private static final List<EntityType> LEASHABLE_MOB_TYPES = Arrays.asList(EntityType.BEE, EntityType.CAT, EntityType.CHICKEN,
            EntityType.COW, EntityType.DOLPHIN, EntityType.DONKEY, EntityType.FOX, EntityType.HOGLIN, EntityType.HORSE, EntityType.SKELETON_HORSE,
            EntityType.ZOMBIE_HORSE, EntityType.IRON_GOLEM, EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.MOOSHROOM,
            EntityType.MULE, EntityType.OCELOT, EntityType.PARROT, EntityType.PIG, EntityType.POLAR_BEAR, EntityType.RABBIT,
            EntityType.SHEEP, EntityType.SNOW_GOLEM, EntityType.STRIDER, EntityType.WOLF, EntityType.ZOGLIN);

    private static final List<EntityType> SADDLEABLE_WHEN_TAMED_MOB_TYPES = Arrays.asList(EntityType.DONKEY, EntityType.HORSE,
            EntityType.ZOMBIE_HORSE, EntityType.MULE);
    /**
     * A list of all foods a wolf can eat on Java Edition.
     * Used to display interactive tag if needed.
     */
    private static final List<String> WOLF_FOODS = Arrays.asList("pufferfish", "tropical_fish", "chicken", "cooked_chicken",
            "porkchop", "beef", "rabbit", "cooked_porkchop", "cooked_beef", "rotten_flesh", "mutton", "cooked_mutton",
            "cooked_rabbit");

    @Override
    public void translate(InteractPacket packet, GeyserSession session) {
        Entity entity;
        if (packet.getRuntimeEntityId() == session.getPlayerEntity().getGeyserId()) {
            //Player is not in entity cache
            entity = session.getPlayerEntity();
        } else {
            entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
        }
        if (entity == null)
            return;

        switch (packet.getAction()) {
            case INTERACT:
                if (session.getInventory().getItem(session.getInventory().getHeldItemSlot() + 36).getId() == ItemRegistry.SHIELD.getJavaId()) {
                    break;
                }
                ClientPlayerInteractEntityPacket interactPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                        InteractAction.INTERACT, Hand.MAIN_HAND, session.isSneaking());
                session.sendDownstreamPacket(interactPacket);
                break;
            case DAMAGE:
                ClientPlayerInteractEntityPacket attackPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                        InteractAction.ATTACK, Hand.MAIN_HAND, session.isSneaking());
                session.sendDownstreamPacket(attackPacket);
                break;
            case LEAVE_VEHICLE:
                ClientPlayerStatePacket sneakPacket = new ClientPlayerStatePacket((int) entity.getEntityId(), PlayerState.START_SNEAKING);
                session.sendDownstreamPacket(sneakPacket);
                session.setRidingVehicleEntity(null);
                break;
            case MOUSEOVER:
                // Handle the buttons for mobile - "Mount", etc; and the suggestions for console - "ZL: Mount", etc
                if (packet.getRuntimeEntityId() != 0) {
                    Entity interactEntity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
                    if (interactEntity == null)
                        return;
                    EntityDataMap entityMetadata = interactEntity.getMetadata();
                    ItemEntry itemEntry = session.getInventory().getItemInHand() == null ? ItemEntry.AIR : ItemRegistry.getItem(session.getInventory().getItemInHand());
                    String javaIdentifierStripped = itemEntry.getJavaIdentifier().replace("minecraft:", "");

                    // TODO - in the future, update these in the metadata? So the client doesn't have to wiggle their cursor around for it to happen
                    // TODO - also, might be good to abstract out the eating thing. I know there will need to be food tracked for https://github.com/GeyserMC/Geyser/issues/1005 but not all food is breeding food
                    InteractiveTag interactiveTag = InteractiveTag.NONE;
                    if (entityMetadata.getLong(EntityData.LEASH_HOLDER_EID) == session.getPlayerEntity().getGeyserId()) {
                        // Unleash the entity
                        interactiveTag = InteractiveTag.REMOVE_LEASH;
                    } else if (javaIdentifierStripped.equals("saddle") && !entityMetadata.getFlags().getFlag(EntityFlag.SADDLED) &&
                            ((SADDLEABLE_WHEN_TAMED_MOB_TYPES.contains(interactEntity.getEntityType()) && entityMetadata.getFlags().getFlag(EntityFlag.TAMED)) ||
                            interactEntity.getEntityType() == EntityType.PIG || interactEntity.getEntityType() == EntityType.STRIDER)) {
                        // Entity can be saddled and the conditions meet (entity can be saddled and, if needed, is tamed)
                        interactiveTag = InteractiveTag.SADDLE;
                    } else if (javaIdentifierStripped.equals("name_tag") && session.getInventory().getItemInHand().getNbt() != null &&
                        session.getInventory().getItemInHand().getNbt().contains("display")) {
                        // Holding a named name tag
                        interactiveTag = InteractiveTag.NAME;
                    } else if (javaIdentifierStripped.equals("lead") && LEASHABLE_MOB_TYPES.contains(interactEntity.getEntityType()) &&
                            entityMetadata.getLong(EntityData.LEASH_HOLDER_EID) == -1L) {
                        // Holding a leash and the mob is leashable for sure
                        // (Plugins can change this behavior so that's something to look into in the far far future)
                        interactiveTag = InteractiveTag.LEASH;
                    } else {
                        switch (interactEntity.getEntityType()) {
                            case BEE:
                                if (FLOWERS.contains(javaIdentifierStripped)) {
                                    interactiveTag = InteractiveTag.FEED;
                                }
                                break;
                            case BOAT:
                                interactiveTag = InteractiveTag.BOARD_BOAT;
                                break;
                            case CAT:
                                if (javaIdentifierStripped.equals("cod") || javaIdentifierStripped.equals("salmon")) {
                                    interactiveTag = InteractiveTag.FEED;
                                } else if (entityMetadata.getFlags().getFlag(EntityFlag.TAMED) &&
                                        entityMetadata.getLong(EntityData.OWNER_EID) == session.getPlayerEntity().getGeyserId()) {
                                    // Tamed and owned by player - can sit/stand
                                    interactiveTag = entityMetadata.getFlags().getFlag(EntityFlag.SITTING) ? InteractiveTag.STAND : InteractiveTag.SIT;
                                    break;
                                }
                                break;
                            case CHICKEN:
                                if (javaIdentifierStripped.contains("seeds")) {
                                    interactiveTag = InteractiveTag.FEED;
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
                                if (javaIdentifierStripped.equals("wheat")) {
                                    interactiveTag = InteractiveTag.FEED;
                                } else if (javaIdentifierStripped.equals("bucket")) {
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
                                // have another switch statement as, while these share mount attributes they don't share food
                                switch (interactEntity.getEntityType()) {
                                    case LLAMA:
                                    case TRADER_LLAMA:
                                        if (javaIdentifierStripped.equals("wheat") || javaIdentifierStripped.equals("hay_block")) {
                                            interactiveTag = InteractiveTag.FEED;
                                            break;
                                        }
                                    case DONKEY:
                                    case HORSE:
                                        // Undead can't eat
                                        if (DONKEY_AND_HORSE_FOODS.contains(javaIdentifierStripped)) {
                                            interactiveTag = InteractiveTag.FEED;
                                            break;
                                        }
                                }
                                if (!entityMetadata.getFlags().getFlag(EntityFlag.BABY)) {
                                    // Can't ride a baby
                                    if (entityMetadata.getFlags().getFlag(EntityFlag.TAMED)) {
                                        interactiveTag = InteractiveTag.RIDE_HORSE;
                                    } else if (!entityMetadata.getFlags().getFlag(EntityFlag.TAMED) && itemEntry.equals(ItemEntry.AIR)) {
                                        // Can't hide an untamed entity without having your hand empty
                                        interactiveTag = InteractiveTag.MOUNT;
                                    }
                                }
                                break;
                            case FOX:
                                if (javaIdentifierStripped.equals("sweet_berries")) {
                                    interactiveTag = InteractiveTag.FEED;
                                }
                                break;
                            case HOGLIN:
                                if (javaIdentifierStripped.equals("crimson_fungus")) {
                                    interactiveTag = InteractiveTag.FEED;
                                }
                                break;
                            case MINECART:
                                interactiveTag = InteractiveTag.RIDE_MINECART;
                                break;
                            case MINECART_CHEST:
                            case MINECART_COMMAND_BLOCK:
                            case MINECART_HOPPER:
                                interactiveTag = InteractiveTag.OPEN_CONTAINER;
                                break;
                            case OCELOT:
                                if (javaIdentifierStripped.equals("cod") || javaIdentifierStripped.equals("salmon")) {
                                    interactiveTag = InteractiveTag.FEED;
                                }
                                break;
                            case PANDA:
                                if (javaIdentifierStripped.equals("bamboo")) {
                                    interactiveTag = InteractiveTag.FEED;
                                }
                                break;
                            case PARROT:
                                if (javaIdentifierStripped.contains("seeds") || javaIdentifierStripped.equals("cookie")) {
                                    interactiveTag = InteractiveTag.FEED;
                                }
                                break;
                            case PIG:
                                if (javaIdentifierStripped.equals("carrot") || javaIdentifierStripped.equals("potato") || javaIdentifierStripped.equals("beetroot")) {
                                    interactiveTag = InteractiveTag.FEED;
                                } else if (entityMetadata.getFlags().getFlag(EntityFlag.SADDLED)) {
                                    interactiveTag = InteractiveTag.MOUNT;
                                }
                                break;
                            case PIGLIN:
                                if (!entityMetadata.getFlags().getFlag(EntityFlag.BABY) && javaIdentifierStripped.equals("gold_ingot")) {
                                    interactiveTag = InteractiveTag.BARTER;
                                }
                                break;
                            case RABBIT:
                                if (javaIdentifierStripped.equals("dandelion") || javaIdentifierStripped.equals("carrot") || javaIdentifierStripped.equals("golden_carrot")) {
                                    interactiveTag = InteractiveTag.FEED;
                                }
                                break;
                            case SHEEP:
                                if (javaIdentifierStripped.equals("wheat")) {
                                    interactiveTag = InteractiveTag.FEED;
                                } else if (!entityMetadata.getFlags().getFlag(EntityFlag.SHEARED)) {
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
                                if (javaIdentifierStripped.equals("warped_fungus")) {
                                    interactiveTag = InteractiveTag.FEED;
                                } else if (entityMetadata.getFlags().getFlag(EntityFlag.SADDLED)) {
                                    interactiveTag = InteractiveTag.RIDE_STRIDER;
                                }
                                break;
                            case TURTLE:
                                if (javaIdentifierStripped.equals("seagrass")) {
                                    interactiveTag = InteractiveTag.FEED;
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
                                } else if (WOLF_FOODS.contains(javaIdentifierStripped)) {
                                    // Compatible food in hand - feed
                                    // Sometimes just sits/stands when the wolf isn't hungry - there doesn't appear to be a way to fix this
                                    interactiveTag = InteractiveTag.FEED;
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
                } else {
                    if (!session.getPlayerEntity().getMetadata().getString(EntityData.INTERACTIVE_TAG).isEmpty()) {
                        // No interactive tag should be sent
                        session.getPlayerEntity().getMetadata().remove(EntityData.INTERACTIVE_TAG);
                        session.getPlayerEntity().updateBedrockMetadata(session);
                    }
                }
                break;
            case OPEN_INVENTORY:
                if (!session.getInventory().isOpen()) {
                    ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
                    containerOpenPacket.setId((byte) 0);
                    containerOpenPacket.setType(ContainerType.INVENTORY);
                    containerOpenPacket.setUniqueEntityId(-1);
                    containerOpenPacket.setBlockPosition(entity.getPosition().toInt());
                    session.sendUpstreamPacket(containerOpenPacket);
                    session.getInventory().setOpen(true);
                }
                break;
        }
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
