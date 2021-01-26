/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.utils;

import com.nukkitx.protocol.bedrock.packet.SetTitlePacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.network.session.GeyserSession;

/**
 * Manages the sending of a cooldown indicator to the Bedrock player as there is no cooldown indicator in Bedrock.
 * Much of the work here is from the wonderful folks from ViaRewind: https://github.com/ViaVersion/ViaRewind
 */
public class CooldownUtils {

    @Getter @Setter
    private static boolean showCooldown;

    /**
     * Starts sending the fake cooldown to the Bedrock client.
     * @param session GeyserSession
     */
    public static void sendCooldown(GeyserSession session) {
        if (!showCooldown) return;
        if (session.getAttackSpeed() == 0.0 || session.getAttackSpeed() > 20) return; // 0.0 usually happens on login and causes issues with visuals; anything above 20 means a plugin like OldCombatMechanics is being used
        // Needs to be sent or no subtitle packet is recognized by the client
        SetTitlePacket titlePacket = new SetTitlePacket();
        titlePacket.setType(SetTitlePacket.Type.TITLE);
        titlePacket.setText(" ");
        session.sendUpstreamPacket(titlePacket);
        session.setLastHitTime(System.currentTimeMillis());
    }

    /**
     * Keeps updating the cooldown until the bar is complete. This should be executed every tick.
     * @param session GeyserSession
     */
    public static void computeCurrentCooldown(GeyserSession session) {
        SetTitlePacket titlePacket = new SetTitlePacket();
        titlePacket.setType(SetTitlePacket.Type.SUBTITLE);
        titlePacket.setText(getTitle(session));
        titlePacket.setFadeInTime(0);
        titlePacket.setFadeOutTime(5);
        titlePacket.setStayTime(2);
        session.sendUpstreamPacket(titlePacket);
        if (!hasCooldown(session)) {
            session.setLastHitTime(-1);
            SetTitlePacket removeTitlePacket = new SetTitlePacket();
            removeTitlePacket.setType(SetTitlePacket.Type.SUBTITLE);
            removeTitlePacket.setText(" ");
            session.sendUpstreamPacket(removeTitlePacket);
        }
    }

    private static boolean hasCooldown(GeyserSession session) {
        long time = System.currentTimeMillis() - session.getLastHitTime();
        double cooldown = restrain(((double) time) * session.getAttackSpeed() / 1000d, 1.5);
        return cooldown < 1.1;
    }


    private static double restrain(double x, double max) {
        if (x < 0d)
            return 0d;
        return Math.min(x, max);
    }

    private static String getTitle(GeyserSession session) {
        long time = System.currentTimeMillis() - session.getLastHitTime();
        double cooldown = restrain(((double) time) * session.getAttackSpeed() / 1000d, 1);

        int darkGrey = (int) Math.floor(10d * cooldown);
        int grey = 10 - darkGrey;
        StringBuilder builder = new StringBuilder("§8");
        while (darkGrey > 0) {
            builder.append("˙");
            darkGrey--;
        }
        builder.append("§7");
        while (grey > 0) {
            builder.append("˙");
            grey--;
        }
        return builder.toString();
    }
}
