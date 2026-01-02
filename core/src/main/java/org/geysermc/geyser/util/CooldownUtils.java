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
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;

/**
 * Manages the sending of a cooldown indicator to the Bedrock player as there is no cooldown indicator in Bedrock.
 * Much of the work here is from the wonderful folks from <a href="https://github.com/ViaVersion/ViaRewind">ViaRewind</a>
 */
public class CooldownUtils {
    /**
     * Sets the last hit time for use when ticking the attack cooldown
     *
     * @param session GeyserSession
     */
    public static void sendCooldown(GeyserSession session) {
        if (session.getGeyser().config().gameplay().showCooldown() == CooldownType.DISABLED) return;
        CooldownType sessionPreference = session.getPreferencesCache().getCooldownPreference();
        if (sessionPreference == CooldownType.DISABLED) return;

        if (session.getAttackSpeed() == 0.0 || session.getAttackSpeed() > 20) {
            return; // 0.0 usually happens on login and causes issues with visuals; anything above 20 means a plugin like OldCombatMechanics is being used
        }

        session.setLastHitTime(System.currentTimeMillis());
    }

    public static String getTitle(GeyserSession session) {
        long time = System.currentTimeMillis() - session.getLastHitTime();
        double tickrateMultiplier = Math.max(session.getMillisecondsPerTick() / 50, 1.0);
        double cooldown = MathUtils.restrain(((double) time) * session.getAttackSpeed() / (tickrateMultiplier * 1000.0), 1.0);

        if (cooldown == 1.0 && session.getMouseoverEntity() != null) {
            return ChatColor.GREEN + "˙".repeat(10);
        }

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

    public static String getIntegratedPackTitle(GeyserSession session, CooldownType sessionPreference) {
        String prefix = sessionPreference.equals(CooldownType.TITLE) ? "geyseropt:cooldown_crosshair" : "geyseropt:cooldown_hotbar";

        long time = System.currentTimeMillis() - session.getLastHitTime();
        double tickrateMultiplier = Math.max(session.getMillisecondsPerTick() / 50, 1.0);
        double cooldown = MathUtils.restrain(((double) time) * session.getAttackSpeed() / (tickrateMultiplier * 1000.0), 1.0);

        int size;
        byte offset;
        if (sessionPreference.equals(CooldownType.TITLE)) {
            size = 17; // Crosshair cooldown has 17 different types
            offset = 0;
        } else {
            size = 18; // Hotbar cooldown has 18 different types
            offset = 18;
        }
        if (cooldown < 1.0) {
            offset += (byte) ((cooldown * size) - 1);
        } else if (session.getMouseoverEntity() != null) {
            offset += (byte) 17; // 17 is the hover one
        } else { // We shouldn't really get to here, but if we do, cooldown is over, just return a space string (thanks bedrock)
            return prefix + " ";
        }
        return prefix + (char) (0xEF00 + offset);
    }

    @Getter
    public enum CooldownType {
        TITLE,
        ACTIONBAR,
        DISABLED;

        public static final CooldownType[] VALUES = values();

        /**
         * Convert the CooldownType string (from config) to the enum, DISABLED on fail
         *
         * @param name CooldownType string
         *
         * @return The converted CooldownType
         */
        public static CooldownType getByName(String name) {
            for (CooldownType type : VALUES) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return DISABLED;
        }
    }
}
