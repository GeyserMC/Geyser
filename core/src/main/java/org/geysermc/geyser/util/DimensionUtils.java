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

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.PlayerActionType;
import com.nukkitx.protocol.bedrock.packet.ChangeDimensionPacket;
import com.nukkitx.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import com.nukkitx.protocol.bedrock.packet.MobEffectPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerActionPacket;
import com.nukkitx.protocol.bedrock.packet.StopSoundPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Set;

public class DimensionUtils {
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

    /**
     * Switches the dimension (Java identifier) for a given Geyser session.
     *
     * @param session Geyser session
     * @param javaDimension Minecraft Java edition dimension identifier
     */
    public static void switchDimension(GeyserSession session, String javaDimension) {
        int bedrockDimension = javaToBedrock(session, javaDimension);
        int previousDimension = javaToBedrock(session, session.getDimension());

        Entity player = session.getPlayerEntity();

        session.getChunkCache().clear();
        session.getEntityCache().removeAllEntities();
        session.getItemFrameCache().clear();
        if (session.getLecternCache() != null) {
            session.getLecternCache().clear();
        }
        session.getLodestoneCache().clear();
        session.getPistonCache().clear();
        session.getSkullCache().clear();

        if (session.getServerRenderDistance() > 32 && !session.isEmulatePost1_13Logic()) {
            // The server-sided view distance wasn't a thing until Minecraft Java 1.14
            // So ViaVersion compensates by sending a "view distance" of 64
            // That's fine, except when the actual view distance sent from the server is five chunks
            // The client locks up when switching dimensions, expecting more chunks than it's getting
            // To solve this, we cap at 32 unless we know that the render distance actually exceeds 32
            // Also, as of 1.19: PS4 crashes with a ChunkRadiusUpdatedPacket too large
            session.getGeyser().getLogger().debug("Applying dimension switching workaround for Bedrock render distance of "
                    + session.getServerRenderDistance());
            ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
            chunkRadiusUpdatedPacket.setRadius(32);
            session.sendUpstreamPacket(chunkRadiusUpdatedPacket);
            // Will be re-adjusted on spawn
        }

        Vector3f pos = Vector3f.from(0, Short.MAX_VALUE, 0);

        ChangeDimensionPacket changeDimensionPacket = new ChangeDimensionPacket();
        changeDimensionPacket.setDimension(bedrockDimension);
        changeDimensionPacket.setRespawn(true);
        changeDimensionPacket.setPosition(pos);
        session.sendUpstreamPacket(changeDimensionPacket);

        session.setDimension(javaDimension);
        setBedrockDimension(session, javaDimension);

        player.setPosition(pos);
        session.setSpawned(false);
        session.setLastChunkPosition(null);

        Set<Effect> entityEffects = session.getEffectCache().getEntityEffects();
        for (Effect effect : entityEffects) {
            MobEffectPacket mobEffectPacket = new MobEffectPacket();
            mobEffectPacket.setEvent(MobEffectPacket.Event.REMOVE);
            mobEffectPacket.setRuntimeEntityId(player.getGeyserId());
            mobEffectPacket.setEffectId(EntityUtils.toBedrockEffectId(effect));
            session.sendUpstreamPacket(mobEffectPacket);
        }
        // Effects are re-sent from server
        entityEffects.clear();

        //let java server handle portal travel sound
        StopSoundPacket stopSoundPacket = new StopSoundPacket();
        stopSoundPacket.setStoppingAllSound(true);
        stopSoundPacket.setSoundName("");
        session.sendUpstreamPacket(stopSoundPacket);

        // Kind of silly but Bedrock 1.19.50 requires an acknowledgement after the
        // initial chunks are sent, prior to the client acknowledgement
        if (GameProtocol.supports1_19_50(session)) {
            // Note: send this before chunks are sent. Fixed https://github.com/GeyserMC/Geyser/issues/3421
            PlayerActionPacket ackPacket = new PlayerActionPacket();
            ackPacket.setRuntimeEntityId(player.getGeyserId());
            ackPacket.setAction(PlayerActionType.DIMENSION_CHANGE_SUCCESS);
            ackPacket.setBlockPosition(Vector3i.ZERO);
            ackPacket.setResultPosition(Vector3i.ZERO);
            ackPacket.setFace(0);
            session.sendUpstreamPacket(ackPacket);
        }

        // TODO - fix this hack of a fix by sending the final dimension switching logic after sections have been sent.
        // The client wants sections sent to it before it can successfully respawn.
        ChunkUtils.sendEmptyChunks(session, player.getPosition().toInt(), 3, true);

        // If the bedrock nether height workaround is enabled, meaning the client is told it's in the end dimension,
        // we check if the player is entering the nether and apply the nether fog to fake the fact that the client
        // thinks they are in the end dimension.
        if (session.getPreferencesCache().allowAboveBedrockNetherBuilding()) {
            if (NETHER.equals(javaDimension)) {
                session.sendFog("minecraft:fog_hell");
            } else if (previousDimension == 2) {
                session.removeFog("minecraft:fog_hell");
            }
        }
    }

    /**
     * Sets the Bedrock dimension in the chunk cache for a given Minecraft dimension identifier (Java).
     *
     * @param session Geyser session (this is used to check whether the "fake" Nether is enabled)
     * @param javaDimension Minecraft Java edition dimension identifier
     */
    public static void setBedrockDimension(GeyserSession session, String javaDimension) {
        session.getChunkCache().setBedrockDimension(switch (javaDimension) {
            case DimensionUtils.THE_END -> BedrockDimension.THE_END;
            case DimensionUtils.NETHER -> session.getPreferencesCache().allowAboveBedrockNetherBuilding() ? BedrockDimension.THE_END : BedrockDimension.THE_NETHER;
            default -> BedrockDimension.OVERWORLD;
        });
    }

    /**
     * Maps a Minecraft Java edition dimension to a Bedrock dimension identifier.
     *
     * @param javaDimension Minecraft Java edition dimension identifier
     * @return Minecraft Bedrock edition dimension identifier
     */
    public static int javaToBedrock(String javaDimension) {
        return switch (javaDimension) {
            case NETHER -> 1;
            case THE_END -> 2;
            default -> 0;
        };
    }

    /**
     * Maps a Minecraft Java edition dimension to a Bedrock dimension identifier.
     *
     * @param session Geyser session (this is used to check whether the "fake" Nether is enabled)
     * @param javaDimension Minecraft Java edition dimension identifier
     * @return Minecraft Bedrock edition dimension identifier
     */
    public static int javaToBedrock(GeyserSession session, String javaDimension) {
        return switch (javaDimension) {
            case NETHER -> session.getPreferencesCache().allowAboveBedrockNetherBuilding() ? 2 : 1;
            case THE_END -> 2;
            default -> 0;
        };
    }

    /**
     * Maps a Minecraft Bedrock edition dimension to a Bedrock dimension identifier.
     *
     * @param session Geyser session (this is used to check whether the "fake" Nether is enabled)
     * @param dimension Minecraft Bedrock edition dimension
     * @return Minecraft Bedrock edition dimension identifier
     */
    public static int javaToBedrock(GeyserSession session, BedrockDimension dimension) {
        if (dimension == BedrockDimension.THE_NETHER) {
            return session.getPreferencesCache().allowAboveBedrockNetherBuilding() ? 2 : 1;
        } else if (dimension == BedrockDimension.THE_END) {
            return 2;
        } else {
            return 0;
        }
    }

    /**
     * Gets the fake, temporary dimension we send clients to so we aren't switching to the same dimension without an additional
     * dimension switch.
     *
     * @param session Geyser session (this is used to check whether the "fake" Nether is enabled)
     * @param currentDimension the current dimension of the player
     * @param newDimension the new dimension that the player will be transferred to
     * @return the fake dimension to transfer to
     */
    public static String getTemporaryDimension(GeyserSession session, String currentDimension, String newDimension) {
        if (session.getPreferencesCache().allowAboveBedrockNetherBuilding()) {
            // Prevents rare instances of Bedrock locking up
            return javaToBedrock(session, newDimension) == 2 ? OVERWORLD : NETHER;
        }
        // Check current Bedrock dimension and not just the Java dimension.
        // Fixes rare instances like https://github.com/GeyserMC/Geyser/issues/3161
        return javaToBedrock(session, currentDimension) == 0 ? NETHER : OVERWORLD;
    }
}
