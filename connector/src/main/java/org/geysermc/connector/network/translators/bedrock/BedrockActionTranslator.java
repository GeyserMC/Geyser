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

package org.geysermc.connector.network.translators.bedrock;

import java.util.concurrent.TimeUnit;

import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerStatePacket;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.PlayStatusPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerActionPacket;
import org.geysermc.connector.network.translators.block.BlockTranslator;

@Translator(packet = PlayerActionPacket.class)
public class BedrockActionTranslator extends PacketTranslator<PlayerActionPacket> {

    @Override
    public void translate(PlayerActionPacket packet, GeyserSession session) {
        Entity entity = session.getPlayerEntity();
        if (entity == null)
            return;

        Vector3i vector = packet.getBlockPosition();
        Position position = new Position(vector.getX(), vector.getY(), vector.getZ());

        switch (packet.getAction()) {
            case RESPAWN:
                // Don't put anything here as respawn is already handled
                // in BedrockRespawnTranslator
                break;
            case START_SWIMMING:
                ClientPlayerStatePacket startSwimPacket = new ClientPlayerStatePacket((int) entity.getEntityId(), PlayerState.START_SPRINTING);
                session.getDownstream().getSession().send(startSwimPacket);
                break;
            case STOP_SWIMMING:
                ClientPlayerStatePacket stopSwimPacket = new ClientPlayerStatePacket((int) entity.getEntityId(), PlayerState.STOP_SPRINTING);
                session.getDownstream().getSession().send(stopSwimPacket);
                break;
            case START_GLIDE:
            case STOP_GLIDE:
                ClientPlayerStatePacket glidePacket = new ClientPlayerStatePacket((int) entity.getEntityId(), PlayerState.START_ELYTRA_FLYING);
                session.getDownstream().getSession().send(glidePacket);
                break;
            case START_SNEAK:
                ClientPlayerStatePacket startSneakPacket = new ClientPlayerStatePacket((int) entity.getEntityId(), PlayerState.START_SNEAKING);
                session.getDownstream().getSession().send(startSneakPacket);
                break;
            case STOP_SNEAK:
                ClientPlayerStatePacket stopSneakPacket = new ClientPlayerStatePacket((int) entity.getEntityId(), PlayerState.STOP_SNEAKING);
                session.getDownstream().getSession().send(stopSneakPacket);
                break;
            case START_SPRINT:
                ClientPlayerStatePacket startSprintPacket = new ClientPlayerStatePacket((int) entity.getEntityId(), PlayerState.START_SPRINTING);
                session.getDownstream().getSession().send(startSprintPacket);
                session.setSprinting(true);
                break;
            case STOP_SPRINT:
                ClientPlayerStatePacket stopSprintPacket = new ClientPlayerStatePacket((int) entity.getEntityId(), PlayerState.STOP_SPRINTING);
                session.getDownstream().getSession().send(stopSprintPacket);
                session.setSprinting(false);
                break;
            case DROP_ITEM:
                ClientPlayerActionPacket dropItemPacket = new ClientPlayerActionPacket(PlayerAction.DROP_ITEM, position, BlockFace.values()[packet.getFace()]);
                session.getDownstream().getSession().send(dropItemPacket);
                break;
            case STOP_SLEEP:
                ClientPlayerStatePacket stopSleepingPacket = new ClientPlayerStatePacket((int) entity.getEntityId(), PlayerState.LEAVE_BED);
                session.getDownstream().getSession().send(stopSleepingPacket);
                break;
            case BLOCK_INTERACT:
                // Handled in BedrockInventoryTransactionTranslator
                break;
            case START_BREAK:
                ClientPlayerActionPacket startBreakingPacket = new ClientPlayerActionPacket(PlayerAction.START_DIGGING, new Position(packet.getBlockPosition().getX(),
                        packet.getBlockPosition().getY(), packet.getBlockPosition().getZ()), BlockFace.values()[packet.getFace()]);
                session.getDownstream().getSession().send(startBreakingPacket);
                break;
            case CONTINUE_BREAK:
                LevelEventPacket continueBreakPacket = new LevelEventPacket();
                continueBreakPacket.setType(LevelEventType.PUNCH_BLOCK);
                continueBreakPacket.setData(BlockTranslator.getBedrockBlockId(session.getBreakingBlock() == null ? BlockTranslator.AIR : session.getBreakingBlock()));
                continueBreakPacket.setPosition(packet.getBlockPosition().toFloat());
                session.getUpstream().sendPacket(continueBreakPacket);
                break;
            case ABORT_BREAK:
                ClientPlayerActionPacket abortBreakingPacket = new ClientPlayerActionPacket(PlayerAction.CANCEL_DIGGING, new Position(packet.getBlockPosition().getX(),
                        packet.getBlockPosition().getY(), packet.getBlockPosition().getZ()), BlockFace.DOWN);
                session.getDownstream().getSession().send(abortBreakingPacket);
                break;
            case STOP_BREAK:
                // Handled in BedrockInventoryTransactionTranslator
                break;
            case DIMENSION_CHANGE_SUCCESS:
                if (session.getPendingDimSwitches().decrementAndGet() == 0) {
                    //sometimes the client doesn't feel like loading
                    PlayStatusPacket spawnPacket = new PlayStatusPacket();
                    spawnPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
                    session.getUpstream().sendPacket(spawnPacket);
                    entity.updateBedrockAttributes(session);
                    session.getEntityCache().updateBossBars();
                }
                break;
            case JUMP:
                session.setJumping(true);
                session.getConnector().getGeneralThreadPool().schedule(() -> {
                    session.setJumping(false);
                }, 1, TimeUnit.SECONDS);
                break;
        }
    }
}
