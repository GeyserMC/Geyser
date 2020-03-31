package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.world.particle.*;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.utils.ParticleUtils;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnParticlePacket;
import com.nukkitx.math.vector.Vector3f;

@Translator(packet = ServerSpawnParticlePacket.class)
public class JavaSpawnParticlePacket extends PacketTranslator<ServerSpawnParticlePacket> {

    @Override
    public void translate(ServerSpawnParticlePacket packet, GeyserSession session) {
        LevelEventPacket particle = new LevelEventPacket();
        switch(packet.getParticle().getType()){
            case BLOCK:
                particle.setType(LevelEventType.DESTROY);
                particle.setData(BlockTranslator.getBedrockBlockId(((BlockParticleData)packet.getParticle().getData()).getBlockState()));
                particle.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                session.getUpstream().sendPacket(particle);
                break;
            case FALLING_DUST:
                //In fact, FallingDustParticle should have data like DustParticle,
                //but in MCProtocol, its data is BlockState(1).
                particle.setType(LevelEventType.PARTICLE_FALLING_DUST);
                particle.setData(BlockTranslator.getBedrockBlockId(((FallingDustParticleData)packet.getParticle().getData()).getBlockState()));
                particle.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                session.getUpstream().sendPacket(particle);
                break;
            case ITEM:
                ItemStack javaItem = ((ItemParticleData)packet.getParticle().getData()).getItemStack();
                ItemData bedrockItem = Translators.getItemTranslator().translateToBedrock(javaItem);
                int id = bedrockItem.getId();
                short damage = bedrockItem.getDamage();
                particle.setType(LevelEventType.PARTICLE_ITEM_BREAK);
                particle.setData(id << 16 | damage);
                particle.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                session.getUpstream().sendPacket(particle);
                break;
            case DUST:
                DustParticleData data = (DustParticleData)packet.getParticle().getData();
                int r = (int) (data.getRed()*255);
                int g = (int) (data.getGreen()*255);
                int b = (int) (data.getBlue()*255);
                particle.setType(LevelEventType.PARTICLE_FALLING_DUST);
                particle.setData(((0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff));
                particle.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                session.getUpstream().sendPacket(particle);
                break;
            default:
                LevelEventType typeParticle = ParticleUtils.getParticleLevelEventType(packet.getParticle().getType());
                if(typeParticle != null){
                    particle.setType(typeParticle);
                    particle.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                    session.getUpstream().sendPacket(particle);
                }else{
                    String stringParticle = ParticleUtils.getParticleString(packet.getParticle().getType());
                    if(stringParticle != null){
                        SpawnParticleEffectPacket stringPacket = new SpawnParticleEffectPacket();
                        stringPacket.setIdentifier(stringParticle);
                        stringPacket.setDimensionId(session.getPlayerEntity().getDimension());
                        stringPacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
                        session.getUpstream().sendPacket(stringPacket);
                    }
                }
                break;
        }
    }

}
