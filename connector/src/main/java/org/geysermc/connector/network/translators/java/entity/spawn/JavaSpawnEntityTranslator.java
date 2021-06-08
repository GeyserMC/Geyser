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

package org.geysermc.connector.network.translators.java.entity.spawn;

import com.github.steveice10.mc.protocol.data.game.entity.object.FallingBlockData;
import com.github.steveice10.mc.protocol.data.game.entity.object.HangingDirection;
import com.github.steveice10.mc.protocol.data.game.entity.object.ProjectileData;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnEntityPacket;
import com.nukkitx.math.vector.Vector3f;
import org.geysermc.connector.entity.*;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.EntityUtils;
import org.geysermc.connector.utils.LanguageUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Translator(packet = ServerSpawnEntityPacket.class)
public class JavaSpawnEntityTranslator extends PacketTranslator<ServerSpawnEntityPacket> {

    @Override
    public void translate(ServerSpawnEntityPacket packet, GeyserSession session) {

        Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
        Vector3f motion = Vector3f.from(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
        Vector3f rotation = Vector3f.from(packet.getYaw(), packet.getPitch(), 0);

        org.geysermc.connector.entity.type.EntityType type = EntityUtils.toBedrockEntity(packet.getType());
        if (type == null) {
            session.getConnector().getLogger().warning(LanguageUtils.getLocaleStringLog("geyser.entity.type_null", packet.getType()));
            return;
        }

        Class<? extends Entity> entityClass = type.getEntityClass();
        try {
            Entity entity;
            if (packet.getType() == EntityType.FALLING_BLOCK) {
                entity = new FallingBlockEntity(packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(),
                        type, position, motion, rotation, ((FallingBlockData) packet.getData()).getId());
            } else if (packet.getType() == EntityType.ITEM_FRAME || packet.getType() == EntityType.GLOW_ITEM_FRAME) {
                // Item frames need the hanging direction
                entity = new ItemFrameEntity(packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(),
                        type, position, motion, rotation, (HangingDirection) packet.getData());
            } else if (packet.getType() == EntityType.FISHING_BOBBER) {
                // Fishing bobbers need the owner for the line
                int ownerEntityId = ((ProjectileData) packet.getData()).getOwnerId();
                Entity owner = session.getEntityCache().getEntityByJavaId(ownerEntityId);
                if (owner == null && session.getPlayerEntity().getEntityId() == ownerEntityId) {
                    owner = session.getPlayerEntity();
                }
                // Java clients only spawn fishing hooks with a player as its owner
                if (owner instanceof PlayerEntity) {
                    entity = new FishingHookEntity(packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(),
                            type, position, motion, rotation, (PlayerEntity) owner);
                } else {
                    return;
                }
            } else if (packet.getType() == EntityType.BOAT) {
                // Initial rotation is incorrect
                entity = new BoatEntity(packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(),
                        type, position, motion, Vector3f.from(packet.getYaw(), 0, packet.getYaw()));
            } else {
                Constructor<? extends Entity> entityConstructor = entityClass.getConstructor(long.class, long.class, org.geysermc.connector.entity.type.EntityType.class,
                        Vector3f.class, Vector3f.class, Vector3f.class);

                entity = entityConstructor.newInstance(packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(),
                        type, position, motion, rotation
                );
            }
            session.getEntityCache().spawnEntity(entity);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
        }
    }
}
