package org.geysermc.geyser.translator.protocol.bedrock.world;

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEvent2Packet;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = LevelSoundEvent2Packet.class)
public class BedrockLevelSoundEvent2Translator extends PacketTranslator<LevelSoundEvent2Packet> {
        
        @Override
        public void translate(GeyserSession session, LevelSoundEvent2Packet packet) {
            session.getGeyser().getLogger().info("LevelSoundEvent2Packet: " + packet.getSound());
            session.sendUpstreamPacket(packet);
            
            // Used by client to get book from lecterns in survial mode since 1.20.70
            Vector3f position = packet.getPosition();
            Vector3i blockPosition = Vector3i.from(position.getX(), position.getY(), position.getZ());

            session.getGeyser().getLogger().info("LevelSoundEvent2Packet: " + packet.getSound() + " at " + blockPosition);

            int potentialLectern = session.getGeyser().getWorldManager().getBlockAt(session, blockPosition);
            session.getGeyser().getLogger().info("LevelSoundEvent2Packet: " + potentialLectern);

            if (BlockStateValues.getLecternBookStates().getOrDefault(potentialLectern, false)) {
                session.getGeyser().getLogger().info("LevelSoundEvent2Packet: " + packet.getSound() + " at " + blockPosition + " is a lectern");
                session.setDroppingLecternBook(true);

                ServerboundUseItemOnPacket blockPacket = new ServerboundUseItemOnPacket(
                        blockPosition,
                        Direction.DOWN,
                        Hand.MAIN_HAND,
                        0, 0, 0,
                        false,
                        session.getWorldCache().nextPredictionSequence());
                session.sendDownstreamGamePacket(blockPacket);
            }
        }
}
