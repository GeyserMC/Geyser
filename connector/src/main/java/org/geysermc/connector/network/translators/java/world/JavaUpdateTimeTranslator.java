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

package org.geysermc.connector.network.translators.java.world;

import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.packet.GameRulesChangedPacket;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.nukkitx.protocol.bedrock.packet.SetTimePacket;

@Translator(packet = ServerUpdateTimePacket.class)
public class JavaUpdateTimeTranslator extends PacketTranslator<ServerUpdateTimePacket> {

    // If negative, the last time is stored so we know it's not some plugin behavior doing weird things.
    // Per-player for multi-world support
    static Long2LongMap lastRecordedTimes = new Long2LongOpenHashMap();

    @Override
    public void translate(ServerUpdateTimePacket packet, GeyserSession session) {

        // Bedrock sends a GameRulesChangedPacket if there is no daylight cycle
        // Java just sends a negative long if there is no daylight cycle
        long lastTime = lastRecordedTimes.getOrDefault(session.getPlayerEntity().getEntityId(), 0);
        long time = packet.getTime();

        if (lastTime != time) {
            // https://minecraft.gamepedia.com/Day-night_cycle#24-hour_Minecraft_day
            SetTimePacket setTimePacket = new SetTimePacket();
            setTimePacket.setTime((int) Math.abs(time) % 24000);
            session.getUpstream().sendPacket(setTimePacket);
            // TODO: Performance efficient to always do this?
            lastRecordedTimes.put(session.getPlayerEntity().getEntityId(), time);
        }
        if (lastTime < 0 && time >= 0) {
            setDoDayLightGamerule(session, true);
        } else if (lastTime != time && time < 0) {
            setDoDayLightGamerule(session, false);
        }
    }

    private void setDoDayLightGamerule(GeyserSession session, boolean doCycle) {
        GameRulesChangedPacket gameRulesChangedPacket = new GameRulesChangedPacket();
        gameRulesChangedPacket.getGameRules().add(new GameRuleData<>("dodaylightcycle", doCycle));
        session.getUpstream().sendPacket(gameRulesChangedPacket);
    }

}
