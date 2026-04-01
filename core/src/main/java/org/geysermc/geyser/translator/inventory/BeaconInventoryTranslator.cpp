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

#include "it.unimi.dsi.fastutil.ints.IntSets"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.BeaconPaymentAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse"
#include "org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket"
#include "org.geysermc.geyser.inventory.BeaconContainer"
#include "org.geysermc.geyser.inventory.BedrockContainerSlot"
#include "org.geysermc.geyser.inventory.Container"
#include "org.geysermc.geyser.inventory.holder.BlockInventoryHolder"
#include "org.geysermc.geyser.inventory.updater.UIInventoryUpdater"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetBeaconPacket"

#include "java.util.OptionalInt"

public class BeaconInventoryTranslator extends AbstractBlockInventoryTranslator<BeaconContainer> {
    public BeaconInventoryTranslator() {
        super(1, new BlockInventoryHolder(Blocks.BEACON, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.BEACON) {
            override protected bool checkInteractionPosition(GeyserSession session) {

                return true;
            }

            override public bool prepareInventory(GeyserSession session, Container container) {

                return super.canUseRealBlock(session, container);
            }
        }, UIInventoryUpdater.INSTANCE);
    }

    override public void updateProperty(GeyserSession session, BeaconContainer container, int key, int value) {


        switch (key) {
            case 0:

                break;
            case 1:
                container.setPrimaryId(value == -1 ? 0 : value);
                break;
            case 2:
                container.setSecondaryId(value == -1 ? 0 : value);
                break;
        }


        Vector3i position = container.getHolderPosition();
        NbtMapBuilder builder = BlockEntityTranslator.getConstantBedrockTag("Beacon", position)
                .putString("CustomName", container.getTitle())
                .putInt("primary", container.getPrimaryId())
                .putInt("secondary", container.getSecondaryId());

        BlockEntityDataPacket packet = new BlockEntityDataPacket();
        packet.setBlockPosition(position);
        packet.setData(builder.build());
        session.sendUpstreamPacket(packet);
    }

    override protected bool shouldHandleRequestFirst(ItemStackRequestAction action, BeaconContainer container) {
        return action.getType() == ItemStackRequestActionType.BEACON_PAYMENT;
    }

    override public ItemStackResponse translateSpecialRequest(GeyserSession session, BeaconContainer container, ItemStackRequest request) {

        BeaconPaymentAction beaconPayment = (BeaconPaymentAction) request.getActions()[0];
        ServerboundSetBeaconPacket packet = new ServerboundSetBeaconPacket(toJava(beaconPayment.getPrimaryEffect()), toJava(beaconPayment.getSecondaryEffect()));
        session.sendDownstreamGamePacket(packet);
        return acceptRequest(request, makeContainerEntries(session, container, IntSets.emptySet()));
    }

    private OptionalInt toJava(int effectChoice) {
        return effectChoice == 0 ? OptionalInt.empty() : OptionalInt.of(effectChoice - 1);
    }

    override public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        if (slotInfoData.getContainerName().getContainer() == ContainerSlotType.BEACON_PAYMENT) {
            return 0;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int slot, BeaconContainer container) {
        if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.BEACON_PAYMENT, 27);
        }
        return super.javaSlotToBedrockContainer(slot, container);
    }

    override public int javaSlotToBedrock(int slot) {
        if (slot == 0) {
            return 27;
        }
        return super.javaSlotToBedrock(slot);
    }

    override public BeaconContainer createInventory(GeyserSession session, std::string name, int windowId, ContainerType containerType) {
        return new BeaconContainer(session, name, windowId, this.size, containerType);
    }

    override public org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType closeContainerType(BeaconContainer container) {
        return null;
    }
}
