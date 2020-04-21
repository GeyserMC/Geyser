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

package org.geysermc.connector.network.translators.java.entity.player;

import java.util.Set;

import com.nukkitx.protocol.bedrock.data.CommandPermission;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import com.nukkitx.protocol.bedrock.data.EntityDataMap;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.packet.AdventureSettingsPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

@Translator(packet = ServerPlayerAbilitiesPacket.class)
public class JavaPlayerAbilitiesTranslator extends PacketTranslator<ServerPlayerAbilitiesPacket> {

    @Override
    public void translate(ServerPlayerAbilitiesPacket packet, GeyserSession session) {
        Entity entity = session.getPlayerEntity();
        if (entity == null)
            return;

        EntityDataMap metadata = entity.getMetadata();
        metadata.getFlags().setFlag(EntityFlag.CAN_FLY, packet.isCanFly());

        SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
        entityDataPacket.setRuntimeEntityId(entity.getGeyserId());
        entityDataPacket.getMetadata().putAll(metadata);
        session.getUpstream().sendPacket(entityDataPacket);

        Set<AdventureSettingsPacket.Flag> playerFlags = new ObjectOpenHashSet<>();
        playerFlags.add(AdventureSettingsPacket.Flag.AUTO_JUMP);
        if (packet.isCanFly())
            playerFlags.add(AdventureSettingsPacket.Flag.MAY_FLY);

        if (packet.isFlying())
            playerFlags.add(AdventureSettingsPacket.Flag.FLYING);

        AdventureSettingsPacket adventureSettingsPacket = new AdventureSettingsPacket();
        adventureSettingsPacket.setPlayerPermission(PlayerPermission.MEMBER);
        adventureSettingsPacket.setCommandPermission(CommandPermission.NORMAL);
        adventureSettingsPacket.setUniqueEntityId(entity.getGeyserId());
        adventureSettingsPacket.getFlags().addAll(playerFlags);
        session.getUpstream().sendPacket(adventureSettingsPacket);
    }
}
