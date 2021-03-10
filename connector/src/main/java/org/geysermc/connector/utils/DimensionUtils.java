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

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.ChangeDimensionPacket;
import com.nukkitx.protocol.bedrock.packet.MobEffectPacket;
import com.nukkitx.protocol.bedrock.packet.StopSoundPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;

public class DimensionUtils {

    // Changes if the above-bedrock Nether building workaround is applied
    private static int BEDROCK_NETHER_ID = 1;

    /**
     * String reference to vanilla Java overworld dimension identifier
     */
    public static final String OVERWORLD = "minecraft:overworld";
    /**
     * String reference to vanilla Java nether dimension identifier
     */
    public static final String NETHER = "minecraft:the_nether";
    /**
     * String reference to vanilla Java end dimension identifier
     */
    public static final String THE_END = "minecraft:the_end";

    public static void switchDimension(GeyserSession session, String javaDimension) {
        int bedrockDimension = javaToBedrock(javaDimension);
        Entity player = session.getPlayerEntity();

        session.getEntityCache().removeAllEntities();
        session.getItemFrameCache().clear();
        session.getLecternCache().clear();
        session.getSkullCache().clear();

        Vector3i pos = Vector3i.from(0, Short.MAX_VALUE, 0);

        ChangeDimensionPacket changeDimensionPacket = new ChangeDimensionPacket();
        changeDimensionPacket.setDimension(bedrockDimension);
        changeDimensionPacket.setRespawn(true);
        changeDimensionPacket.setPosition(pos.toFloat());
        session.sendUpstreamPacket(changeDimensionPacket);
        session.setDimension(javaDimension);
        player.setPosition(pos.toFloat());
        session.setSpawned(false);
        session.setLastChunkPosition(null);

        for (Effect effect : session.getEffectCache().getEntityEffects().keySet()) {
            MobEffectPacket mobEffectPacket = new MobEffectPacket();
            mobEffectPacket.setEvent(MobEffectPacket.Event.REMOVE);
            mobEffectPacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
            mobEffectPacket.setEffectId(EntityUtils.toBedrockEffectId(effect));
            session.sendUpstreamPacket(mobEffectPacket);
        }
        // Effects are re-sent from server
        session.getEffectCache().getEntityEffects().clear();

        //let java server handle portal travel sound
        StopSoundPacket stopSoundPacket = new StopSoundPacket();
        stopSoundPacket.setStoppingAllSound(true);
        stopSoundPacket.setSoundName("");
        session.sendUpstreamPacket(stopSoundPacket);

        // TODO - fix this hack of a fix by sending the final dimension switching logic after chunks have been sent.
        // The client wants chunks sent to it before it can successfully respawn.
        ChunkUtils.sendEmptyChunks(session, player.getPosition().toInt(), 3, true);
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

    /**
     * Determines the new dimension based on the {@link CompoundTag} sent by either the {@link com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket}
     * or {@link com.github.steveice10.mc.protocol.packet.ingame.server.ServerRespawnPacket}.
     *
     * @param dimensionTag the packet's dimension tag.
     * @return the dimension identifier.
     */
    public static String getNewDimension(CompoundTag dimensionTag) {
        if (dimensionTag == null || dimensionTag.isEmpty()) {
            GeyserConnector.getInstance().getLogger().debug("Dimension tag was null or empty.");
            return OVERWORLD;
        }
        if (dimensionTag.getValue().get("effects") != null) {
            return ((StringTag) dimensionTag.getValue().get("effects")).getValue();
        }
        GeyserConnector.getInstance().getLogger().debug("Effects portion of the tag was null or empty.");
        return OVERWORLD;
    }

    /**
     * The Nether dimension in Bedrock does not permit building above Y128 - the Bedrock above the dimension.
     * This workaround sets the Nether as the End dimension to ignore this limit.
     *
     * @param isAboveNetherBedrockBuilding true if we should apply The End workaround
     */
    public static void changeBedrockNetherId(boolean isAboveNetherBedrockBuilding) {
        // Change dimension ID to the End to allow for building above Bedrock
        BEDROCK_NETHER_ID = isAboveNetherBedrockBuilding ? 2 : 1;
    }

    /**
     * Gets the fake, temporary dimension we send clients to so we aren't switching to the same dimension without an additional
     * dimension switch.
     *
     * @param currentDimension the current dimension of the player
     * @param newDimension the new dimension that the player will be transferred to
     * @return the fake dimension to transfer to
     */
    public static String getTemporaryDimension(String currentDimension, String newDimension) {
        if (BEDROCK_NETHER_ID == 2) {
            // Prevents rare instances of Bedrock locking up
            return javaToBedrock(newDimension) == 2 ? OVERWORLD : NETHER;
        }
        return currentDimension.equals(OVERWORLD) ? NETHER : OVERWORLD;
    }
}
