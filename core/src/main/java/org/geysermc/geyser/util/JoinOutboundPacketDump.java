/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityProperty;
import org.cloudburstmc.protocol.bedrock.data.entity.FloatEntityProperty;
import org.cloudburstmc.protocol.bedrock.data.entity.IntEntityProperty;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.AddItemEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.AvailableCommandsPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.cloudburstmc.protocol.bedrock.packet.UnlockedRecipesPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.StringJoiner;

/**
 * Formats outbound Bedrock packets for join-sequence crash debugging.
 * Keeps a short ring buffer so disconnect can print the last packets sent.
 */
public final class JoinOutboundPacketDump {
    private static final int RECENT_LIMIT = 48;

    private final ArrayDeque<String> recent = new ArrayDeque<>(RECENT_LIMIT);
    private int outboundCount;

    public String describe(BedrockPacket packet) {
        String name = packet.getClass().getSimpleName();
        String detail = detail(packet);
        String line = detail.isEmpty() ? name : name + " " + detail;
        remember(line);
        outboundCount++;
        return line;
    }

    public int outboundCount() {
        return outboundCount;
    }

    public String recentSummary() {
        if (recent.isEmpty()) {
            return "(none)";
        }
        StringJoiner joiner = new StringJoiner(" | ");
        for (String entry : recent) {
            joiner.add(entry);
        }
        return joiner.toString();
    }

    private void remember(String line) {
        if (recent.size() >= RECENT_LIMIT) {
            recent.removeFirst();
        }
        recent.addLast(line);
    }

    private static String detail(BedrockPacket packet) {
        return switch (packet) {
            case AddEntityPacket p -> {
                StringBuilder sb = new StringBuilder();
                sb.append("id=").append(p.getIdentifier())
                    .append(" runtime=").append(p.getRuntimeEntityId())
                    .append(" pos=").append(fmt(p.getPosition()))
                    .append(" meta=").append(metaSize(p.getMetadata()));
                appendProperties(sb, p.getProperties().getIntProperties(), p.getProperties().getFloatProperties());
                yield sb.toString();
            }
            case AddItemEntityPacket p -> "runtime=" + p.getRuntimeEntityId()
                + " item=" + itemSummary(p.getItemInHand())
                + " pos=" + fmt(p.getPosition());
            case AddPlayerPacket p -> "name=" + p.getUsername()
                + " runtime=" + p.getRuntimeEntityId()
                + " pos=" + fmt(p.getPosition())
                + " meta=" + metaSize(p.getMetadata());
            case SetEntityDataPacket p -> {
                StringBuilder sb = new StringBuilder();
                sb.append("runtime=").append(p.getRuntimeEntityId())
                    .append(" meta=").append(metaSize(p.getMetadata()));
                appendProperties(sb, p.getProperties().getIntProperties(), p.getProperties().getFloatProperties());
                yield sb.toString();
            }
            case SetEntityMotionPacket p -> "runtime=" + p.getRuntimeEntityId()
                + " motion=" + fmt(p.getMotion())
                + " tick=" + p.getTick();
            case LevelChunkPacket p -> "chunk=" + p.getChunkX() + "," + p.getChunkZ()
                + " sub=" + p.getSubChunksLength()
                + " bytes=" + (p.getData() == null ? 0 : p.getData().readableBytes());
            case CraftingDataPacket p -> "recipes=" + p.getCraftingData().size()
                + " potions=" + p.getPotionMixData().size()
                + " containers=" + p.getContainerMixData().size()
                + " clear=" + p.isCleanRecipes();
            case UnlockedRecipesPacket p -> "action=" + p.getAction()
                + " count=" + p.getUnlockedRecipes().size()
                + sample(p.getUnlockedRecipes(), 5);
            case InventoryContentPacket p -> "container=" + p.getContainerId()
                + " slots=" + p.getContents().size();
            case InventorySlotPacket p -> "container=" + p.getContainerId()
                + " slot=" + p.getSlot()
                + " item=" + itemSummary(p.getItem());
            case LevelSoundEventPacket p -> "sound=" + p.getSound()
                + " id=" + p.getIdentifier()
                + " extra=" + p.getExtraData()
                + " pos=" + fmt(p.getPosition());
            case MobEquipmentPacket p -> "runtime=" + p.getRuntimeEntityId()
                + " slot=" + p.getInventorySlot()
                + " hotbar=" + p.getHotbarSlot()
                + " item=" + itemSummary(p.getItem());
            case MoveEntityAbsolutePacket p -> "runtime=" + p.getRuntimeEntityId()
                + " pos=" + fmt(p.getPosition())
                + " onGround=" + p.isOnGround();
            case MoveEntityDeltaPacket p -> "runtime=" + p.getRuntimeEntityId()
                + " flags=" + p.getFlags();
            case MovePlayerPacket p -> "runtime=" + p.getRuntimeEntityId()
                + " mode=" + p.getMode()
                + " pos=" + fmt(p.getPosition());
            case UpdateAttributesPacket p -> "runtime=" + p.getRuntimeEntityId()
                + " attrs=" + p.getAttributes().size();
            case RemoveEntityPacket p -> "unique=" + p.getUniqueEntityId();
            case PlayerListPacket p -> "action=" + p.getAction()
                + " entries=" + p.getEntries().size();
            case AvailableCommandsPacket p -> "commands=" + p.getCommands().size();
            case TextPacket p -> "type=" + p.getType()
                + " msgLen=" + (p.getMessage() == null ? 0 : p.getMessage().length());
            case RespawnPacket p -> "state=" + p.getState()
                + " pos=" + fmt(p.getPosition());
            case SetTimePacket p -> "time=" + p.getTime();
            default -> "";
        };
    }

    private static void appendProperties(StringBuilder sb, Collection<IntEntityProperty> ints,
                                         Collection<FloatEntityProperty> floats) {
        if ((ints == null || ints.isEmpty()) && (floats == null || floats.isEmpty())) {
            return;
        }
        sb.append(" propsInt=").append(formatProps(ints));
        sb.append(" propsFloat=").append(formatProps(floats));
    }

    private static String formatProps(Collection<? extends EntityProperty> props) {
        if (props == null || props.isEmpty()) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(props.size()).append('{');
        boolean first = true;
        for (EntityProperty prop : props) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(prop.getIndex()).append('=');
            if (prop instanceof IntEntityProperty intProp) {
                sb.append(intProp.getValue());
            } else if (prop instanceof FloatEntityProperty floatProp) {
                sb.append(floatProp.getValue());
            } else {
                sb.append('?');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static int metaSize(EntityDataMap metadata) {
        return metadata == null ? 0 : metadata.size();
    }

    private static String itemSummary(ItemData item) {
        if (item == null || item == ItemData.AIR) {
            return "air";
        }
        String def = item.getDefinition() == null ? "?" : item.getDefinition().getIdentifier();
        return def + "*" + item.getCount() + (item.getDamage() != 0 ? "#" + item.getDamage() : "");
    }

    private static String fmt(Vector3f v) {
        if (v == null) {
            return "null";
        }
        return String.format("%.2f,%.2f,%.2f", v.getX(), v.getY(), v.getZ());
    }

    private static String sample(Iterable<String> values, int limit) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        int n = 0;
        for (String value : values) {
            if (n++ >= limit) {
                joiner.add("...");
                break;
            }
            joiner.add(value);
        }
        return joiner.toString();
    }
}
