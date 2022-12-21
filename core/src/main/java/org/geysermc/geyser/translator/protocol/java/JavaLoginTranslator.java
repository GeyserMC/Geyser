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

package org.geysermc.geyser.translator.protocol.java;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.packet.AdventureSettingsPacket;
import com.nukkitx.protocol.bedrock.packet.GameRulesChangedPacket;
import com.nukkitx.protocol.bedrock.packet.SetPlayerGameTypePacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannels;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.TextDecoration;
import org.geysermc.geyser.translator.level.BiomeTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.util.JavaCodecUtil;
import org.geysermc.geyser.util.PluginMessageUtils;

import java.util.Map;

@Translator(packet = ClientboundLoginPacket.class)
public class JavaLoginTranslator extends PacketTranslator<ClientboundLoginPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundLoginPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        entity.setEntityId(packet.getEntityId());

        Map<String, JavaDimension> dimensions = session.getDimensions();
        dimensions.clear();

        JavaDimension.load(packet.getRegistry(), dimensions);

        Int2ObjectMap<TextDecoration> chatTypes = session.getChatTypes();
        chatTypes.clear();
        for (CompoundTag tag : JavaCodecUtil.iterateAsTag(packet.getRegistry().get("minecraft:chat_type"))) {
            // The ID is NOT ALWAYS THE SAME! ViaVersion as of 1.19 adds two registry entries that do NOT match vanilla.
            int id = ((IntTag) tag.get("id")).getValue();
            CompoundTag element = tag.get("element");
            CompoundTag chat = element.get("chat");
            TextDecoration textDecoration = null;
            if (chat != null) {
                textDecoration = new TextDecoration(chat);
            }
            chatTypes.put(id, textDecoration);
        }

        // If the player is already initialized and a join game packet is sent, they
        // are swapping servers
        if (session.isSpawned()) {
            String fakeDim = DimensionUtils.getTemporaryDimension(session.getDimension(), packet.getDimension());
            DimensionUtils.switchDimension(session, fakeDim);

            session.getWorldCache().removeScoreboard();
        }
        session.setWorldName(packet.getWorldName());
        session.setLevels(packet.getWorldNames());

        BiomeTranslator.loadServerBiomes(session, packet.getRegistry());
        session.getTagCache().clear();

        session.setGameMode(packet.getGameMode());

        String newDimension = packet.getDimension();

        boolean needsSpawnPacket = !session.isSentSpawnPacket();
        if (needsSpawnPacket) {
            // The player has yet to spawn so let's do that using some of the information in this Java packet
            session.setDimension(newDimension);
            DimensionUtils.setBedrockDimension(session, newDimension);
            session.connect();

            // It is now safe to send these packets
            session.getUpstream().sendPostStartGamePackets();
        }

        AdventureSettingsPacket bedrockPacket = new AdventureSettingsPacket();
        bedrockPacket.setUniqueEntityId(session.getPlayerEntity().getGeyserId());
        bedrockPacket.setPlayerPermission(PlayerPermission.MEMBER);
        session.sendUpstreamPacket(bedrockPacket);

        if (!needsSpawnPacket) {
            SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
            playerGameTypePacket.setGamemode(packet.getGameMode().ordinal());
            session.sendUpstreamPacket(playerGameTypePacket);
        }

        entity.setLastDeathPosition(packet.getLastDeathPos());

        entity.updateBedrockMetadata();

        // Send if client should show respawn screen
        GameRulesChangedPacket gamerulePacket = new GameRulesChangedPacket();
        gamerulePacket.getGameRules().add(new GameRuleData<>("doimmediaterespawn", !packet.isEnableRespawnScreen()));
        session.sendUpstreamPacket(gamerulePacket);

        session.setReducedDebugInfo(packet.isReducedDebugInfo());

        session.setServerRenderDistance(packet.getViewDistance());

        // TODO customize
        session.sendJavaClientSettings();

        session.sendDownstreamPacket(new ServerboundCustomPayloadPacket("minecraft:brand", PluginMessageUtils.getGeyserBrandData()));

        // register the plugin messaging channels used in Floodgate
        if (session.remoteServer().authType() == AuthType.FLOODGATE) {
            session.sendDownstreamPacket(new ServerboundCustomPayloadPacket("minecraft:register", PluginMessageChannels.getFloodgateRegisterData()));
        }

        if (!newDimension.equals(session.getDimension())) {
            DimensionUtils.switchDimension(session, newDimension);
        } else if (DimensionUtils.isCustomBedrockNetherId() && newDimension.equalsIgnoreCase(DimensionUtils.NETHER)) {
            // If the player is spawning into the "fake" nether, send them some fog
            session.sendFog("minecraft:fog_hell");
        }

        ChunkUtils.loadDimension(session);
    }
}
