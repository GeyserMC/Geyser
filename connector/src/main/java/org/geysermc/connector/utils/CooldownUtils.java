/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.utils;

import com.nukkitx.protocol.bedrock.packet.SetTitlePacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.concurrent.TimeUnit;

/**
 * Manages the sending of a cooldown indicator to the Bedrock player as there is no cooldown indicator in Bedrock.
 * Much of the work here is from the wonderful folks from ViaRewind: https://github.com/ViaVersion/ViaRewind
 */
public class CooldownUtils {

    private final static boolean SHOW_COOLDOWN;

    static {
        SHOW_COOLDOWN = GeyserConnector.getInstance().getConfig().isShowCooldown();
    }

    /**
     * Starts sending the fake cooldown to the Bedrock client.
     * @param session GeyserSession
     */
    public static void sendCooldown(GeyserSession session) {
        if (!SHOW_COOLDOWN) return;
        if (session.getAttackSpeed() == 0.0 || session.getAttackSpeed() > 20) return; // 0.0 usually happens on login and causes issues with visuals; anything above 20 means a plugin like OldCombatMechanics is being used
        // Needs to be sent or no subtitle packet is recognized by the client
        SetTitlePacket titlePacket = new SetTitlePacket();
        titlePacket.setType(SetTitlePacket.Type.SET_TITLE);
        titlePacket.setText(" ");
        session.sendUpstreamPacket(titlePacket);
        session.setLastHitTime(System.currentTimeMillis());
        long lastHitTime = session.getLastHitTime(); // Used later to prevent multiple scheduled cooldown threads
        computeCooldown(session, lastHitTime);
    }

    /**
     * Keeps updating the cooldown until the bar is complete.
     * @param session GeyserSession
     * @param lastHitTime The time of the last hit. Used to gauge how long the cooldown is taking.
     */
    private static void computeCooldown(GeyserSession session, long lastHitTime) {
        if (session.isClosed()) return; // Don't run scheduled tasks if the client left
        if (lastHitTime != session.getLastHitTime()) return; // Means another cooldown has started so there's no need to continue this one
        SetTitlePacket titlePacket = new SetTitlePacket();
        titlePacket.setType(SetTitlePacket.Type.SET_SUBTITLE);
        titlePacket.setText(getTitle(session));
        titlePacket.setFadeInTime(0);
        titlePacket.setFadeOutTime(5);
        titlePacket.setStayTime(2);
        session.sendUpstreamPacket(titlePacket);
        if (hasCooldown(session)) {
            session.getConnector().getGeneralThreadPool().schedule(() -> computeCooldown(session, lastHitTime), 50, TimeUnit.MILLISECONDS); // Updated per tick. 1000 divided by 20 ticks equals 50
        } else {
            SetTitlePacket removeTitlePacket = new SetTitlePacket();
            removeTitlePacket.setType(SetTitlePacket.Type.SET_SUBTITLE);
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
