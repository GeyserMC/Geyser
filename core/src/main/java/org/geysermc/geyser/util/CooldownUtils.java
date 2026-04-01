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

package org.geysermc.geyser.util;

import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;


public class CooldownUtils {
    
    public static void setCooldownHitTime(GeyserSession session) {
        if (session.getPreferencesCache().getCooldownPreference() == CooldownType.DISABLED) {
            return;
        }
        if (session.getAttackSpeed() == 0.0 || session.getAttackSpeed() > 20) {
            return; 
        }

        session.setLastHitTime(System.currentTimeMillis());
    }

    public static void tickCooldown(GeyserSession session) {
        if (session.getGameMode().equals(GameMode.SPECTATOR)) return; 

        CooldownType sessionPreference = session.getPreferencesCache().getCooldownPreference();
        if (sessionPreference == CooldownType.DISABLED) return;

        if (session.getAttackSpeed() == 0.0 || session.getAttackSpeed() > 20) {
            clearCooldown(session); 
            return; 
        }

        long time = System.currentTimeMillis() - session.getLastHitTime();
        double tickrateMultiplier = Math.max(session.getMillisecondsPerTick() / 50, 1.0);
        double cooldown = MathUtils.restrain(((double) time) * session.getAttackSpeed() / (tickrateMultiplier * 1000.0), 1.0);

        if (cooldown < 1.0) {
            sendCooldown(session, sessionPreference, cooldown);
        } else if (session.isNeedAttackCooldownClear()) {
            clearCooldown(session);
        }
    }

    public static void sendCooldown(GeyserSession session, CooldownType sessionPreference, double cooldown) {
        if (session.integratedPackActive()) {
            String value = (sessionPreference == CooldownType.CROSSHAIR ? "crs" : "htb") + ":" + Math.round(cooldown * 16);
            session.sendJsonUIData("cooldown", value);
        } else {
            SetTitlePacket titlePacket;

            
            if (!session.getWorldCache().isTitleTimesIncorrect()) {
                
                titlePacket = new SetTitlePacket();
                titlePacket.setType(SetTitlePacket.Type.TIMES);
                titlePacket.setStayTime(1000);
                titlePacket.setText("");
                titlePacket.setXuid("");
                titlePacket.setPlatformOnlineId("");
                session.sendUpstreamPacket(titlePacket);

                session.getWorldCache().markTitleTimesAsIncorrect();
            }

            
            if (sessionPreference == CooldownType.CROSSHAIR) {
                
                titlePacket = new SetTitlePacket();
                titlePacket.setType(SetTitlePacket.Type.TITLE);
                titlePacket.setText(" ");
                titlePacket.setXuid("");
                titlePacket.setPlatformOnlineId("");
                session.sendUpstreamPacket(titlePacket);
            }

            titlePacket = new SetTitlePacket();
            if (sessionPreference == CooldownType.HOTBAR) {
                titlePacket.setType(SetTitlePacket.Type.ACTIONBAR);
            } else {
                titlePacket.setType(SetTitlePacket.Type.SUBTITLE);
            }
            titlePacket.setText(CooldownUtils.getTitle(cooldown));
            titlePacket.setXuid("");
            titlePacket.setPlatformOnlineId("");
            session.sendUpstreamPacket(titlePacket);
        }

        session.setNeedAttackCooldownClear(true);
    }

    public static void clearCooldown(GeyserSession session) {
        if (session.integratedPackActive()) {
            session.sendJsonUIData("cooldown", "non");
        } else {
            SetTitlePacket removeTitlePacket = new SetTitlePacket();
            removeTitlePacket.setType(SetTitlePacket.Type.CLEAR);
            removeTitlePacket.setText(" ");
            removeTitlePacket.setXuid("");
            removeTitlePacket.setPlatformOnlineId("");
            session.sendUpstreamPacket(removeTitlePacket);
        }

        session.setNeedAttackCooldownClear(false);
    }

    public static String getTitle(double cooldown) {
        int darkGrey = (int) Math.floor(10d * cooldown);
        int grey = 10 - darkGrey;
        StringBuilder builder = new StringBuilder(ChatColor.DARK_GRAY);
        while (darkGrey > 0) {
            builder.append("˙");
            darkGrey--;
        }
        builder.append(ChatColor.GRAY);
        while (grey > 0) {
            builder.append("˙");
            grey--;
        }
        return builder.toString();
    }

    @Getter
    public enum CooldownType {
        CROSSHAIR,
        HOTBAR,
        DISABLED;

        public static final CooldownType[] VALUES = values();
    }
}
