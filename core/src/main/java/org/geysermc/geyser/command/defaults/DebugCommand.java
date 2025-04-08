/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.command.defaults;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.chest.SingleChestInventoryTranslator;
import org.incendo.cloud.context.CommandContext;

public class DebugCommand extends GeyserCommand {

    public DebugCommand(@NonNull String name, @NonNull String description, @NonNull String permission, @Nullable TriState permissionDefault, boolean playerOnly, boolean bedrockOnly) {
        super(name, description, permission, permissionDefault, playerOnly, bedrockOnly);
    }

    @Override
    public void execute(CommandContext<GeyserCommandSource> context) {
        GeyserSession session = GeyserImpl.getInstance().getSessionManager().getAllSessions().get(0);

        if (session != null) {

            var holder = session.getOpenInventory();
            if (holder != null && holder.translator() instanceof SingleChestInventoryTranslator) {
                for (int i = 0; i <= holder.inventory().getSize(); i++) {
                    int bedrockSlot = holder.translator().javaSlotToBedrock(i);
                    GeyserImpl.getInstance().getLogger().info("java/bedrock slot: " + bedrockSlot);
                    InventorySlotPacket slotPacket = new InventorySlotPacket();
                    slotPacket.setContainerId(ContainerId.INVENTORY);
                    slotPacket.setSlot(bedrockSlot);
                    slotPacket.setItem(ItemData.builder()
                            .count(bedrockSlot)
                            .definition(session.getItemMappings().getStoredItems().glassBottle().getBedrockDefinition())
                        .build());
                    session.sendUpstreamPacket(slotPacket);
                }
            }
        }
    }
}
