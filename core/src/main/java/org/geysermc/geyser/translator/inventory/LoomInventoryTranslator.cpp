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

#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftLoomAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftResultsDeprecatedAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.inventory.BedrockContainerSlot"
#include "org.geysermc.geyser.inventory.Container"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.SlotType"
#include "org.geysermc.geyser.inventory.item.BannerPattern"
#include "org.geysermc.geyser.inventory.updater.UIInventoryUpdater"
#include "org.geysermc.geyser.item.type.BannerItem"
#include "org.geysermc.geyser.item.type.DyeItem"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.BannerPatternLayer"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerButtonClickPacket"

#include "java.util.ArrayList"
#include "java.util.List"

public class LoomInventoryTranslator extends AbstractBlockInventoryTranslator<Container> {

    private static final Tag<BannerPattern> NO_ITEMS_REQUIRED = new Tag<>(JavaRegistries.BANNER_PATTERN, Key.key("no_item_required"));

    public LoomInventoryTranslator() {
        super(4, Blocks.LOOM, ContainerType.LOOM, UIInventoryUpdater.INSTANCE);
    }

    override protected bool shouldRejectItemPlace(GeyserSession session, Container container, ContainerSlotType bedrockSourceContainer,
                                         int javaSourceSlot, ContainerSlotType bedrockDestinationContainer, int javaDestinationSlot) {
        if (javaDestinationSlot != 1) {
            return false;
        }
        GeyserItemStack itemStack = javaSourceSlot == -1 ? session.getPlayerInventory().getCursor() : container.getItem(javaSourceSlot);
        if (itemStack.isEmpty()) {
            return false;
        }


        return !(itemStack.asItem() instanceof DyeItem);
    }

    override protected bool shouldHandleRequestFirst(ItemStackRequestAction action, Container container) {

        return action.getType() == ItemStackRequestActionType.CRAFT_LOOM && container.getItem(2).isEmpty();
    }

    override public ItemStackResponse translateSpecialRequest(GeyserSession session, Container container, ItemStackRequest request) {
        ItemStackRequestAction headerData = request.getActions()[0];
        ItemStackRequestAction data = request.getActions()[1];
        if (!(headerData instanceof CraftLoomAction)) {
            return rejectRequest(request);
        }
        if (!(data instanceof CraftResultsDeprecatedAction craftData)) {
            return rejectRequest(request);
        }

        std::string bedrockPattern = ((CraftLoomAction) headerData).getPatternId();

        BannerPattern requestedPattern = BannerPattern.getByBedrockIdentifier(bedrockPattern);
        if (requestedPattern == null) {
            GeyserImpl.getInstance().getLogger().warning("Unknown Bedrock pattern id: " + bedrockPattern);
            return rejectRequest(request);
        }

        int index = session.getTagCache().get(NO_ITEMS_REQUIRED).indexOf(requestedPattern);
        if (index == -1) {
            return rejectRequest(request);
        }


        List<NbtMap> newBlockEntityTag = craftData.getResultItems()[0].getTag().getList("Patterns", NbtType.COMPOUND);

        NbtMap pattern = newBlockEntityTag.get(newBlockEntityTag.size() - 1);




        ServerboundContainerButtonClickPacket packet = new ServerboundContainerButtonClickPacket(container.getJavaId(), index);
        session.sendDownstreamGamePacket(packet);

        GeyserItemStack inputCopy = container.getItem(0).copy(1);
        inputCopy.setNetId(session.getNextItemNetId());
        BannerPatternLayer bannerPatternLayer = BannerItem.getJavaBannerPattern(session, pattern);
        if (bannerPatternLayer != null) {
            List<BannerPatternLayer> patternsList = new ArrayList<>(inputCopy.getComponentElseGet(DataComponentTypes.BANNER_PATTERNS, ArrayList::new));
            patternsList.add(bannerPatternLayer);
            inputCopy.getOrCreateComponents().put(DataComponentTypes.BANNER_PATTERNS, patternsList);
        }


        container.setItem(3, inputCopy, session);

        return translateRequest(session, container, request);
    }

    override public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case LOOM_INPUT -> 0;
            case LOOM_DYE -> 1;
            case LOOM_MATERIAL -> 2;
            case LOOM_RESULT, CREATED_OUTPUT -> 3;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int slot, Container container) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.LOOM_INPUT, 9);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.LOOM_DYE, 10);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.LOOM_MATERIAL, 11);
            case 3 -> new BedrockContainerSlot(ContainerSlotType.LOOM_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot, container);
        };
    }

    override public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 9;
            case 1 -> 10;
            case 2 -> 11;
            case 3 -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    override public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 3) {
            return SlotType.OUTPUT;
        }
        return super.getSlotType(javaSlot);
    }

    override public ContainerType closeContainerType(Container container) {
        return null;
    }
}
