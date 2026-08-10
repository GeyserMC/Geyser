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

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSets;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.BeaconPaymentAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.geysermc.geyser.inventory.BeaconContainer;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.holder.BlockInventoryHolder;
import org.geysermc.geyser.inventory.updater.UIInventoryUpdater;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetBeaconPacket;

import java.util.OptionalInt;

public class BeaconInventoryTranslator extends AbstractBlockInventoryTranslator<BeaconContainer> {
    /**
     * The level a beacon needs before Java hands out each effect, mirroring BeaconBlockEntity#BEACON_EFFECTS.
     * Anything else isn't a beacon effect at all, so it asks for a level no beacon can reach.
     */
    private static final Int2IntMap EFFECT_LEVELS = new Int2IntOpenHashMap();

    static {
        EFFECT_LEVELS.defaultReturnValue(Integer.MAX_VALUE);
        EFFECT_LEVELS.put(Effect.SPEED.ordinal(), 1);
        EFFECT_LEVELS.put(Effect.HASTE.ordinal(), 1);
        EFFECT_LEVELS.put(Effect.RESISTANCE.ordinal(), 2);
        EFFECT_LEVELS.put(Effect.JUMP_BOOST.ordinal(), 2);
        EFFECT_LEVELS.put(Effect.STRENGTH.ordinal(), 3);
        EFFECT_LEVELS.put(Effect.REGENERATION.ordinal(), 4);
    }

    public BeaconInventoryTranslator() {
        super(1, new BlockInventoryHolder(Blocks.BEACON, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.BEACON) {
            @Override
            protected boolean checkInteractionPosition(GeyserSession session) {
                // Since we can't fall back to a virtual inventory, let's make opening one easier
                return true;
            }

            @Override
            public boolean prepareInventory(GeyserSession session, Container container) {
                // Virtual beacon inventories aren't possible - we don't want to spawn a whole pyramid!
                return super.canUseRealBlock(session, container);
            }
        }, UIInventoryUpdater.INSTANCE);
    }

    @Override
    public void updateProperty(GeyserSession session, BeaconContainer container, int key, int value) {
        //FIXME?: Beacon graphics look weird after inputting an item. This might be a Bedrock bug, since it resets to nothing
        // on BDS
        switch (key) {
            case 0:
                // Bedrock works the level out itself, but Java checks a payment against its own
                container.setLevels(value);
                break;
            case 1:
                container.setPrimaryId(value == -1 ? 0 : value);
                break;
            case 2:
                container.setSecondaryId(value == -1 ? 0 : value);
                break;
        }

        // Send a block entity data packet update to the fake beacon inventory
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

    @Override
    protected boolean shouldHandleRequestFirst(ItemStackRequestAction action, BeaconContainer container) {
        return action.getType() == ItemStackRequestActionType.BEACON_PAYMENT;
    }

    @Override
    public ItemStackResponse translateSpecialRequest(GeyserSession session, BeaconContainer container, ItemStackRequest request) {
        // Input a beacon payment
        BeaconPaymentAction beaconPayment = (BeaconPaymentAction) request.getActions()[0];
        OptionalInt primary = toJava(beaconPayment.getPrimaryEffect());
        OptionalInt secondary = toJava(beaconPayment.getSecondaryEffect());
        if (!validEffects(primary, secondary, container.getLevels())) {
            // Java stops updating the level while the beam is obstructed, so Bedrock can offer effects
            // that Java still thinks the beacon is too small for
            return rejectRequest(request, false);
        }

        ServerboundSetBeaconPacket packet = new ServerboundSetBeaconPacket(primary, secondary);
        session.sendDownstreamGamePacket(packet);
        return acceptRequest(request, makeContainerEntries(session, container, IntSets.emptySet()));
    }

    private OptionalInt toJava(int effectChoice) {
        return effectChoice == 0 ? OptionalInt.empty() : OptionalInt.of(effectChoice - 1);
    }

    private static int requiredLevels(OptionalInt effect) {
        return effect.isEmpty() ? 0 : EFFECT_LEVELS.get(effect.getAsInt());
    }

    /**
     * Mirrors BeaconBlockEntity#validateEffects - Java disconnects us if we ask for effects it wouldn't allow.
     */
    private static boolean validEffects(OptionalInt primary, OptionalInt secondary, int levels) {
        if (secondary.isPresent() && levels < 4) {
            return false;
        }
        int primaryLevels = requiredLevels(primary);
        int secondaryLevels = requiredLevels(secondary);
        if (primaryLevels > levels || secondaryLevels > levels) {
            return false;
        }
        if (primaryLevels >= 4) {
            return false;
        }
        return secondaryLevels == 0 || secondaryLevels >= 4 || primary.equals(secondary);
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        if (slotInfoData.getContainerName().getContainer() == ContainerSlotType.BEACON_PAYMENT) {
            return 0;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot, BeaconContainer container) {
        if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.BEACON_PAYMENT, 27);
        }
        return super.javaSlotToBedrockContainer(slot, container);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot == 0) {
            return 27;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public BeaconContainer createInventory(GeyserSession session, String name, int windowId, ContainerType containerType) {
        return new BeaconContainer(session, name, windowId, this.size, containerType);
    }

    @Override
    public org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType closeContainerType(BeaconContainer container) {
        return null;
    }
}
