/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.inventory.translators;

import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientClickWindowButtonPacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.CraftResultsDeprecatedStackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionType;
import com.nukkitx.protocol.bedrock.packet.ItemStackRequestPacket;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.BedrockContainerSlot;
import org.geysermc.connector.network.translators.inventory.updater.UIInventoryUpdater;

import java.util.List;

public class LoomInventoryTranslator extends AbstractBlockInventoryTranslator {
    /**
     * A map of Bedrock patterns to Java index. Used to request for a specific banner pattern.
     */
    private static final Object2IntMap<String> PATTERN_TO_INDEX = new Object2IntOpenHashMap<>();

    static {
        // Added from left-to-right then up-to-down in the order Java presents it
        int index = 1;
        PATTERN_TO_INDEX.put("bl", index++);
        PATTERN_TO_INDEX.put("br", index++);
        PATTERN_TO_INDEX.put("tl", index++);
        PATTERN_TO_INDEX.put("tr", index++);
        PATTERN_TO_INDEX.put("bs", index++);
        PATTERN_TO_INDEX.put("ts", index++);
        PATTERN_TO_INDEX.put("ls", index++);
        PATTERN_TO_INDEX.put("rs", index++);
        PATTERN_TO_INDEX.put("cs", index++);
        PATTERN_TO_INDEX.put("ms", index++);
        PATTERN_TO_INDEX.put("drs", index++);
        PATTERN_TO_INDEX.put("dls", index++);
        PATTERN_TO_INDEX.put("ss", index++);
        PATTERN_TO_INDEX.put("cr", index++);
        PATTERN_TO_INDEX.put("sc", index++);
        PATTERN_TO_INDEX.put("bt", index++);
        PATTERN_TO_INDEX.put("tt", index++);
        PATTERN_TO_INDEX.put("bts", index++);
        PATTERN_TO_INDEX.put("tts", index++);
        PATTERN_TO_INDEX.put("ld", index++);
        PATTERN_TO_INDEX.put("rd", index++);
        PATTERN_TO_INDEX.put("lud", index++);
        PATTERN_TO_INDEX.put("rud", index++);
        PATTERN_TO_INDEX.put("mc", index++);
        PATTERN_TO_INDEX.put("mr", index++);
        PATTERN_TO_INDEX.put("vh", index++);
        PATTERN_TO_INDEX.put("hh", index++);
        PATTERN_TO_INDEX.put("vhr", index++);
        PATTERN_TO_INDEX.put("hhb", index++);
        PATTERN_TO_INDEX.put("bo", index++);
        index++; // Bordure indented, does not appear to exist in Bedrock?
        PATTERN_TO_INDEX.put("gra", index++);
        PATTERN_TO_INDEX.put("gru", index);
        // Bricks do not appear to be a pattern on Bedrock, either
    }

    public LoomInventoryTranslator() {
        super(4, "minecraft:loom[facing=north]", ContainerType.LOOM, UIInventoryUpdater.INSTANCE);
    }

    @Override
    public boolean shouldHandleRequestFirst(StackRequestActionData action, Inventory inventory) {
        // If the LOOM_MATERIAL slot is not empty, we are crafting a pattern that does not come from an item
        return action.getType() == StackRequestActionType.CRAFT_NON_IMPLEMENTED_DEPRECATED && inventory.getItem(2).isEmpty();
    }

    @Override
    public ItemStackResponsePacket.Response translateSpecialRequest(GeyserSession session, Inventory inventory, ItemStackRequestPacket.Request request) {
        // TODO: I anticipate this will be changed in the future to use something non-deprecated. Keep an eye out.
        // Also TODO: Shift-clicking doesn't work here.
        StackRequestActionData data = request.getActions()[1];
        if (!(data instanceof CraftResultsDeprecatedStackRequestActionData)) {
            return rejectRequest(request);
        }
        CraftResultsDeprecatedStackRequestActionData craftData = (CraftResultsDeprecatedStackRequestActionData) data;
        // Get the patterns compound tag
        List<NbtMap> blockEntityTag = craftData.getResultItems()[0].getTag().getList("Patterns", NbtType.COMPOUND);
        // Get the pattern that the Bedrock client requests - the last pattern in the Patterns list
        String pattern = blockEntityTag.get(blockEntityTag.size() - 1).getString("Pattern");
        // Get the Java index of this pattern
        int index = PATTERN_TO_INDEX.getOrDefault(pattern, -1);
        if (index == -1) {
            return rejectRequest(request);
        }
        // Java's formula: 4 * row + col
        // And the Java loom window has a fixed row/width of four
        // So... Number / 4 = row (so we don't have to bother there), and number % 4 is our column, which leads us back to our index. :)
        ClientClickWindowButtonPacket packet = new ClientClickWindowButtonPacket(inventory.getId(), index);
        System.out.println(packet);
        session.sendDownstreamPacket(packet);

        return translateRequest(session, inventory, request);
    }

    @Override
    public int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData) {
        if (slotInfoData.getContainer() == ContainerSlotType.LOOM_INPUT) {
            return 0;
        }
        if (slotInfoData.getContainer() == ContainerSlotType.LOOM_DYE) {
            return 1;
        }
        if (slotInfoData.getContainer() == ContainerSlotType.LOOM_MATERIAL) {
            return 2;
        }
        if (slotInfoData.getContainer() == ContainerSlotType.LOOM_RESULT || slotInfoData.getContainer() == ContainerSlotType.CREATIVE_OUTPUT) {
            return 3;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        switch (slot) {
            case 0:
                return new BedrockContainerSlot(ContainerSlotType.LOOM_INPUT, 9);
            case 1:
                return new BedrockContainerSlot(ContainerSlotType.LOOM_DYE, 10);
            case 2:
                return new BedrockContainerSlot(ContainerSlotType.LOOM_MATERIAL, 11);
            case 3:
                return new BedrockContainerSlot(ContainerSlotType.LOOM_RESULT, 50);
        }
        return super.javaSlotToBedrockContainer(slot);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        switch (slot) {
            case 0:
                return 9;
            case 1:
                return 10;
            case 2:
                return 11;
            case 3:
                return 50;
        }
        return super.javaSlotToBedrock(slot);
    }
}
