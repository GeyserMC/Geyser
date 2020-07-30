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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;

public class DimensionUtils {

    // Changes if the above-bedrock Nether building workaround is applied
    private static int BEDROCK_NETHER_ID = 1;

    // Static references to all vanilla dimensions
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String THE_END = "minecraft:the_end";

    public static void switchDimension(GeyserSession session, String javaDimension) {
        int bedrockDimension = javaToBedrock(javaDimension);
        Entity player = session.getPlayerEntity();
        if (javaDimension.equals(player.getDimension()))
            return;

        session.getEntityCache().removeAllEntities();
        session.getItemFrameCache().clear();
        session.getSkullCache().clear();
        if (session.getPendingDimSwitches().getAndIncrement() > 0) {
            ChunkUtils.sendEmptyChunks(session, player.getPosition().toInt(), 3, true);
        }

        Vector3i pos = Vector3i.from(0, Short.MAX_VALUE, 0);

        ChangeDimensionPacket changeDimensionPacket = new ChangeDimensionPacket();
        changeDimensionPacket.setDimension(bedrockDimension);
        changeDimensionPacket.setRespawn(true);
        changeDimensionPacket.setPosition(pos.toFloat());
        session.sendUpstreamPacket(changeDimensionPacket);
        player.setDimension(javaDimension);
        player.setPosition(pos.toFloat());
        session.setSpawned(false);
        session.setLastChunkPosition(null);

        for (Effect effect : session.getPlayerEntity().getEffectCache().getEntityEffects().keySet()) {
            MobEffectPacket mobEffectPacket = new MobEffectPacket();
            mobEffectPacket.setEvent(MobEffectPacket.Event.REMOVE);
            mobEffectPacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
            mobEffectPacket.setEffectId(EntityUtils.toBedrockEffectId(effect));
            session.sendUpstreamPacket(mobEffectPacket);
        }
        // Effects are re-sent from server
        session.getPlayerEntity().getEffectCache().getEntityEffects().clear();

        //let java server handle portal travel sound
        StopSoundPacket stopSoundPacket = new StopSoundPacket();
        stopSoundPacket.setStoppingAllSound(true);
        stopSoundPacket.setSoundName("");
        session.sendUpstreamPacket(stopSoundPacket);
    }

    /**
     * Map the Java edition dimension IDs to Bedrock edition
     *
     * @param javaDimension Dimension ID to convert
     * @return Converted Bedrock edition dimension ID
     */
    public static int javaToBedrock(String javaDimension) {
        switch (javaDimension) {
            case NETHER:
                return BEDROCK_NETHER_ID;
            case THE_END:
                return 2;
            default:
                return 0;
        }
    }

    public static void changeBedrockNetherId() {
        // Change dimension ID to the End to allow for building above Bedrock
        BEDROCK_NETHER_ID = 2;
    }
}
