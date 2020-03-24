package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.world.particle.*;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.utils.ParticleUtils;
import org.slf4j.LoggerFactory;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnParticlePacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.SpawnParticleEffectPacket;

public class JavaSpawnParticlePacket extends PacketTranslator<ServerSpawnParticlePacket> {

    @Override
    public void translate(ServerSpawnParticlePacket packet, GeyserSession session) {
        switch(packet.getParticle().getType()){
            case BLOCK:
                LevelEventPacket blockParticlePacket = new LevelEventPacket();
                blockParticlePacket.setType(LevelEventType.DESTROY);
                blockParticlePacket.setData(BlockTranslator.getBedrockBlockId(((BlockParticleData)packet.getParticle().getData()).getBlockState()));
                blockParticlePacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                session.getUpstream().sendPacket(blockParticlePacket);
                break;
            case FALLING_DUST:
                LevelEventPacket fallingParticlePacket = new LevelEventPacket();
                fallingParticlePacket.setType(LevelEventType.PARTICLE_FALLING_DUST);
                fallingParticlePacket.setData(BlockTranslator.getBedrockBlockId(((FallingDustParticleData)packet.getParticle().getData()).getBlockState()));
                fallingParticlePacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                break;
            case ITEM:
                ItemStack javaItem = ((ItemParticleData)packet.getParticle().getData()).getItemStack();
                ItemData bedrockItem = TranslatorsInit.getItemTranslator().translateToBedrock(javaItem);
                int id = bedrockItem.getId();
                short damage = bedrockItem.getDamage();
                LevelEventPacket itemParticlePacket = new LevelEventPacket();
                itemParticlePacket.setType(LevelEventType.PARTICLE_ITEM_BREAK);
                itemParticlePacket.setData(id << 16 | damage);
                itemParticlePacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                session.getUpstream().sendPacket(itemParticlePacket);
                break;
            case DUST:
                DustParticleData data = (DustParticleData)packet.getParticle().getData();
                int r = (int) (data.getRed()*255);
                int g = (int) (data.getGreen()*255);
                int b = (int) (data.getBlue()*255);
                LevelEventPacket dustParticlePacket = new LevelEventPacket();
                dustParticlePacket.setType(LevelEventType.PARTICLE_FALLING_DUST);
                dustParticlePacket.setData(((0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff));
                dustParticlePacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                session.getUpstream().sendPacket(dustParticlePacket);
                break;
            default:
                if(ParticleUtils.hasIdentifier(packet.getParticle().getType())){
                    SpawnParticleEffectPacket particlePacket = new SpawnParticleEffectPacket();
                    particlePacket.setDimensionId(session.getPlayerEntity().getDimension());
                    particlePacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                    particlePacket.setIdentifier(ParticleUtils.getIdentifier(packet.getParticle().getType()));
                    session.getUpstream().sendPacket(particlePacket);
                }else {
                    LoggerFactory.getLogger(this.getClass()).debug("No particle mapping for " + packet.getParticle().getType());
                }
                break;
        }
    }

}
