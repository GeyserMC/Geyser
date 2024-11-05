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

package org.geysermc.geyser.translator.protocol.bedrock.world;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.CooldownUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;

@Translator(packet = LevelSoundEventPacket.class)
public class BedrockLevelSoundEventTranslator extends PacketTranslator<LevelSoundEventPacket> {

    @Override
    public void translate(GeyserSession session, LevelSoundEventPacket packet) {
        // lol what even :thinking:
        session.sendUpstreamPacket(packet);

        // Yes, what even, but thankfully we can hijack this packet to send the cooldown
        if (packet.getSound() == SoundEvent.ATTACK_NODAMAGE || packet.getSound() == SoundEvent.ATTACK || packet.getSound() == SoundEvent.ATTACK_STRONG) {
            // Send a faux cooldown since Bedrock has no cooldown support
            // Sent here because Java still sends a cooldown if the player doesn't hit anything but Bedrock always sends a sound
            CooldownUtils.sendCooldown(session);
        }

        if (packet.getSound() == SoundEvent.ATTACK_NODAMAGE && session.getArmAnimationTicks() == -1) {
            // https://github.com/GeyserMC/Geyser/issues/2113
            // Seems like consoles and Android with keyboard send the animation packet on 1.19.51, hence the animation
            // tick check - the animate packet is sent first.
            // ATTACK_NODAMAGE = player clicked air
            // This should only be revisited if Bedrock packets get full Java parity, or Bedrock starts sending arm
            // animation packets after ATTACK_NODAMAGE, OR ATTACK_NODAMAGE gets removed/isn't sent in the same spot
            session.sendDownstreamGamePacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
            session.activateArmAnimationTicking();

            // Send packet to Bedrock so it knows
            AnimatePacket animatePacket = new AnimatePacket();
            animatePacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
            animatePacket.setAction(AnimatePacket.Action.SWING_ARM);
            session.sendUpstreamPacket(animatePacket);
        }

        // Used by client to get book from lecterns in survial mode since 1.20.70
        if (packet.getSound() == SoundEvent.HIT) {
            Vector3f position = packet.getPosition();
            Vector3i blockPosition = Vector3i.from(position.getX(), position.getY(), position.getZ());

            BlockState potentialLectern = session.getGeyser().getWorldManager().blockAt(session, blockPosition);

            if (potentialLectern.getValue(Properties.HAS_BOOK, false)) {
                session.setDroppingLecternBook(true);

                ServerboundUseItemOnPacket blockPacket = new ServerboundUseItemOnPacket(
                        blockPosition,
                        Direction.DOWN,
                        Hand.MAIN_HAND,
                        0, 0, 0,
                        false,
                        false,
                        session.getWorldCache().nextPredictionSequence());
                session.sendDownstreamGamePacket(blockPacket);
            }
        }
    }
}
