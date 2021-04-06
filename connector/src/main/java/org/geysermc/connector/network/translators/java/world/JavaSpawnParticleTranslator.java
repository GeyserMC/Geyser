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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.world.particle.BlockParticleData;
import com.github.steveice10.mc.protocol.data.game.world.particle.DustParticleData;
import com.github.steveice10.mc.protocol.data.game.world.particle.FallingDustParticleData;
import com.github.steveice10.mc.protocol.data.game.world.particle.ItemParticleData;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnParticlePacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.effect.EffectRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.DimensionUtils;

@Translator(packet = ServerSpawnParticlePacket.class)
public class JavaSpawnParticleTranslator extends PacketTranslator<ServerSpawnParticlePacket> {

    @Override
    public void translate(ServerSpawnParticlePacket packet, GeyserSession session) {
        LevelEventPacket particle = new LevelEventPacket();
        switch (packet.getParticle().getType()) {
            case BLOCK:
                particle.setType(LevelEventType.PARTICLE_DESTROY_BLOCK_NO_SOUND);
                particle.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                particle.setData(session.getBlockTranslator().getBedrockBlockId(((BlockParticleData) packet.getParticle().getData()).getBlockState()));
                session.sendUpstreamPacket(particle);
                break;
            case FALLING_DUST:
                //In fact, FallingDustParticle should have data like DustParticle,
                //but in MCProtocol, its data is BlockState(1).
                particle.setType(LevelEventType.PARTICLE_FALLING_DUST);
                particle.setData(session.getBlockTranslator().getBedrockBlockId(((FallingDustParticleData)packet.getParticle().getData()).getBlockState()));
                particle.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                session.sendUpstreamPacket(particle);
                break;
            case ITEM:
                ItemStack javaItem = ((ItemParticleData)packet.getParticle().getData()).getItemStack();
                ItemData bedrockItem = ItemTranslator.translateToBedrock(session, javaItem);
                int id = bedrockItem.getId();
                int damage = bedrockItem.getDamage();
                particle.setType(LevelEventType.PARTICLE_ITEM_BREAK);
                particle.setData(id << 16 | damage);
                particle.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                session.sendUpstreamPacket(particle);
                break;
            case DUST:
                DustParticleData data = (DustParticleData)packet.getParticle().getData();
                int r = (int) (data.getRed()*255);
                int g = (int) (data.getGreen()*255);
                int b = (int) (data.getBlue()*255);
                particle.setType(LevelEventType.PARTICLE_FALLING_DUST);
                particle.setData(((0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff));
                particle.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                session.sendUpstreamPacket(particle);
                break;
            default:
                LevelEventType typeParticle = EffectRegistry.getParticleLevelEventType(packet.getParticle().getType());
                if (typeParticle != null) {
                    particle.setType(typeParticle);
                    particle.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                    session.sendUpstreamPacket(particle);
                } else {
                    String stringParticle = EffectRegistry.getParticleString(packet.getParticle().getType());
                    if (stringParticle != null) {
                        SpawnParticleEffectPacket stringPacket = new SpawnParticleEffectPacket();
                        stringPacket.setIdentifier(stringParticle);
                        stringPacket.setDimensionId(DimensionUtils.javaToBedrock(session.getDimension()));
                        stringPacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                        session.sendUpstreamPacket(stringPacket);
                    }
                }
                break;
        }
    }

}