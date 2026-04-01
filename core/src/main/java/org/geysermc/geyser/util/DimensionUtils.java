/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.packet.ChangeDimensionPacket;
import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;
import org.cloudburstmc.protocol.bedrock.packet.StopSoundPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.level.EffectType;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

import java.util.Set;

public class DimensionUtils {

    public static final String BEDROCK_FOG_HELL = "minecraft:fog_hell";

    public static void switchDimension(GeyserSession session, JavaDimension javaDimension) {
        switchDimension(session, javaDimension, javaDimension.bedrockId());
    }

    public static void switchDimension(GeyserSession session, JavaDimension javaDimension, int bedrockDimension) {
        @Nullable JavaDimension previousDimension = session.getDimensionType(); 

        Entity player = session.getPlayerEntity();

        session.getChunkCache().clear();
        session.getEntityCache().removeAllEntities();
        session.getItemFrameCache().clear();
        session.getLodestoneCache().clear();
        session.getPistonCache().clear();
        session.getSkullCache().clear();
        session.getBlockBreakHandler().reset();

        changeDimension(session, bedrockDimension);

        session.setDimensionType(javaDimension);

        Set<Effect> entityEffects = session.getEffectCache().getEntityEffects();
        for (Effect effect : entityEffects) {
            MobEffectPacket mobEffectPacket = new MobEffectPacket();
            mobEffectPacket.setEvent(MobEffectPacket.Event.REMOVE);
            mobEffectPacket.setRuntimeEntityId(player.geyserId());
            mobEffectPacket.setEffectId(EffectType.fromJavaEffect(effect).getBedrockId());
            session.sendUpstreamPacket(mobEffectPacket);
        }
        
        entityEffects.clear();

        
        session.updateRain(0);
        session.updateThunder(0);

        finalizeDimensionSwitch(session, player);

        
        
        
        if (BedrockDimension.isCustomBedrockNetherId()) {
            if (javaDimension.isNetherLike()) {
                session.camera().sendFog(BEDROCK_FOG_HELL);
            } else if (previousDimension != null && previousDimension.isNetherLike()) {
                session.camera().removeFog(BEDROCK_FOG_HELL);
            }
        }
    }

    
    public static void fastSwitchDimension(GeyserSession session, int bedrockDimension) {
        changeDimension(session, bedrockDimension);
        finalizeDimensionSwitch(session, session.getPlayerEntity());
    }

    private static void changeDimension(GeyserSession session, int bedrockDimension) {
        if (session.getServerRenderDistance() > 32 && !session.isEmulatePost1_13Logic()) {
            
            
            
            
            
            
            session.getGeyser().getLogger().debug("Applying dimension switching workaround for Bedrock render distance of "
                + session.getServerRenderDistance());
            ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
            chunkRadiusUpdatedPacket.setRadius(32);
            session.sendUpstreamPacket(chunkRadiusUpdatedPacket);
            
        }

        Vector3f pos = Vector3f.from(0, Short.MAX_VALUE, 0);

        ChangeDimensionPacket changeDimensionPacket = new ChangeDimensionPacket();
        changeDimensionPacket.setDimension(bedrockDimension);
        changeDimensionPacket.setRespawn(true);
        changeDimensionPacket.setPosition(pos);
        session.sendUpstreamPacket(changeDimensionPacket);

        setBedrockDimension(session, bedrockDimension);

        session.getPlayerEntity().setPosition(pos);
        session.getPlayerEntity().setMotion(Vector3f.ZERO);
        session.getPlayerEntity().setLastTickEndVelocity(Vector3f.ZERO);
        session.setSpawned(false);
        session.setLastChunkPosition(null);
    }

    private static void finalizeDimensionSwitch(GeyserSession session, Entity player) {
        //let java server handle portal travel sound
        StopSoundPacket stopSoundPacket = new StopSoundPacket();
        stopSoundPacket.setStoppingAllSound(true);
        stopSoundPacket.setSoundName("");
        session.sendUpstreamPacket(stopSoundPacket);

        
        
        
        PlayerActionPacket ackPacket = new PlayerActionPacket();
        ackPacket.setRuntimeEntityId(player.geyserId());
        ackPacket.setAction(PlayerActionType.DIMENSION_CHANGE_SUCCESS);
        ackPacket.setBlockPosition(Vector3i.ZERO);
        ackPacket.setResultPosition(Vector3i.ZERO);
        ackPacket.setFace(0);
        session.sendUpstreamPacket(ackPacket);

        
        
        ChunkUtils.sendEmptyChunks(session, player.position().toInt(), 3, true);
    }

    public static void setBedrockDimension(GeyserSession session, int bedrockDimension) {
        session.setBedrockDimension(switch (bedrockDimension) {
            case BedrockDimension.END_ID -> BedrockDimension.THE_END;
            case BedrockDimension.DEFAULT_NETHER_ID -> BedrockDimension.THE_NETHER; 
            default -> session.getBedrockOverworldDimension();
        });
    }

     
    public static int javaToBedrock(String javaDimension) {
        return switch (javaDimension) {
            case BedrockDimension.NETHER_IDENTIFIER -> BedrockDimension.BEDROCK_NETHER_ID;
            case "minecraft:the_end" -> BedrockDimension.END_ID;
            default -> BedrockDimension.OVERWORLD_ID;
        };
    }

    
    public static int javaToBedrock(GeyserSession session) {
        JavaDimension dimension = session.getDimensionType();
        if (dimension == null) {
            return BedrockDimension.OVERWORLD_ID;
        }
        return dimension.bedrockId();
    }

    
    public static int getTemporaryDimension(int currentBedrockDimension, int newBedrockDimension) {
        if (BedrockDimension.isCustomBedrockNetherId()) {
            
            return newBedrockDimension == BedrockDimension.END_ID ? BedrockDimension.OVERWORLD_ID : BedrockDimension.END_ID;
        }
        
        
        return currentBedrockDimension == BedrockDimension.OVERWORLD_ID ? BedrockDimension.DEFAULT_NETHER_ID : BedrockDimension.OVERWORLD_ID;
    }

}
