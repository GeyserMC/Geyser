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

package org.geysermc.geyser.translator.protocol.java.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCooldownPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerStartItemCooldownPacket;
import org.geysermc.geyser.inventory.item.StoredItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundCooldownPacket.class)
public class JavaCooldownTranslator extends PacketTranslator<ClientboundCooldownPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundCooldownPacket packet) {
        StoredItemMappings itemMappings = session.getItemMappings().getStoredItems();

        int itemId = packet.getItemId();
        // Not every item, as of 1.19, appears to be server-driven. Just these two.
        // Use a map here if it gets too big.
        String cooldownCategory;
        if (itemId == itemMappings.goatHorn()) {
            cooldownCategory = "goat_horn";
        } else if (itemId == itemMappings.shield().getJavaId()) {
            cooldownCategory = "shield";
        } else {
            cooldownCategory = null;
        }

        if (cooldownCategory != null) {
            PlayerStartItemCooldownPacket bedrockPacket = new PlayerStartItemCooldownPacket();
            bedrockPacket.setItemCategory(cooldownCategory);
            bedrockPacket.setCooldownDuration(packet.getCooldownTicks());
            session.sendUpstreamPacket(bedrockPacket);
        }
    }
}
