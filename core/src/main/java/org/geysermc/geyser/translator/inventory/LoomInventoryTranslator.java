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

package org.geysermc.geyser.translator.inventory;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftLoomAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftResultsDeprecatedAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.SlotType;
import org.geysermc.geyser.inventory.updater.UIInventoryUpdater;
import org.geysermc.geyser.item.type.BannerItem;
import org.geysermc.geyser.item.type.DyeItem;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.BannerPatternLayer;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerButtonClickPacket;

import java.util.ArrayList;
import java.util.List;

public class LoomInventoryTranslator extends AbstractBlockInventoryTranslator {
    /**
     * A map of Bedrock patterns to Java index. Used to request for a specific banner pattern.
     */
    private static final Object2IntMap<String> PATTERN_TO_INDEX = new Object2IntOpenHashMap<>();

    static {
        // Added from left-to-right then up-to-down in the order Java presents it
        int index = 0;
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
        super(4, Blocks.LOOM, ContainerType.LOOM, UIInventoryUpdater.INSTANCE);
    }

    @Override
    protected boolean shouldRejectItemPlace(GeyserSession session, Inventory inventory, ContainerSlotType bedrockSourceContainer,
                                         int javaSourceSlot, ContainerSlotType bedrockDestinationContainer, int javaDestinationSlot) {
        if (javaDestinationSlot != 1) {
            return false;
        }
        GeyserItemStack itemStack = javaSourceSlot == -1 ? session.getPlayerInventory().getCursor() : inventory.getItem(javaSourceSlot);
        if (itemStack.isEmpty()) {
            return false;
        }

        // Reject the item if Bedrock is attempting to put in a dye that is not a dye in Java Edition
        return !(itemStack.asItem() instanceof DyeItem);
    }

    @Override
    protected boolean shouldHandleRequestFirst(ItemStackRequestAction action, Inventory inventory) {
        // If the LOOM_MATERIAL slot is not empty, we are crafting a pattern that does not come from an item
        return action.getType() == ItemStackRequestActionType.CRAFT_LOOM && inventory.getItem(2).isEmpty();
    }

    @Override
    public ItemStackResponse translateSpecialRequest(GeyserSession session, Inventory inventory, ItemStackRequest request) {
        ItemStackRequestAction headerData = request.getActions()[0];
        ItemStackRequestAction data = request.getActions()[1];
        if (!(headerData instanceof CraftLoomAction)) {
            return rejectRequest(request);
        }
        if (!(data instanceof CraftResultsDeprecatedAction craftData)) {
            return rejectRequest(request);
        }

        // Get the patterns compound tag
        List<NbtMap> newBlockEntityTag = craftData.getResultItems()[0].getTag().getList("Patterns", NbtType.COMPOUND);
        // Get the pattern that the Bedrock client requests - the last pattern in the Patterns list
        NbtMap pattern = newBlockEntityTag.get(newBlockEntityTag.size() - 1);
        String bedrockPattern = ((CraftLoomAction) headerData).getPatternId();

        // Get the Java index of this pattern
        int index = PATTERN_TO_INDEX.getOrDefault(bedrockPattern, -1);
        if (index == -1) {
            return rejectRequest(request);
        }
        // Java's formula: 4 * row + col
        // And the Java loom window has a fixed row/width of four
        // So... Number / 4 = row (so we don't have to bother there), and number % 4 is our column, which leads us back to our index. :)
        ServerboundContainerButtonClickPacket packet = new ServerboundContainerButtonClickPacket(inventory.getJavaId(), index);
        session.sendDownstreamGamePacket(packet);

        GeyserItemStack inputCopy = inventory.getItem(0).copy(1);
        inputCopy.setNetId(session.getNextItemNetId());
        BannerPatternLayer bannerPatternLayer = BannerItem.getJavaBannerPattern(session, pattern); // TODO
        if (bannerPatternLayer != null) {
            List<BannerPatternLayer> patternsList = inputCopy.getComponentOrFallback(DataComponentType.BANNER_PATTERNS, new ArrayList<>());
            patternsList.add(bannerPatternLayer);
            inputCopy.getOrCreateComponents().put(DataComponentType.BANNER_PATTERNS, patternsList);
        }

        // Set the new item as the output
        inventory.setItem(3, inputCopy, session);

        return translateRequest(session, inventory, request);
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainer()) {
            case LOOM_INPUT -> 0;
            case LOOM_DYE -> 1;
            case LOOM_MATERIAL -> 2;
            case LOOM_RESULT, CREATED_OUTPUT -> 3;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.LOOM_INPUT, 9);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.LOOM_DYE, 10);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.LOOM_MATERIAL, 11);
            case 3 -> new BedrockContainerSlot(ContainerSlotType.LOOM_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot);
        };
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 9;
            case 1 -> 10;
            case 2 -> 11;
            case 3 -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 3) {
            return SlotType.OUTPUT;
        }
        return super.getSlotType(javaSlot);
    }
}
