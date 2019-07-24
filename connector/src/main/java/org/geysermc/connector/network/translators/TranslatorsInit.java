/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators;

import com.flowpowered.math.vector.Vector3f;
import com.github.steveice10.mc.protocol.data.message.TranslationMessage;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerTitlePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTOutputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.utils.MessageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class TranslatorsInit {

    private static final CompoundTag EMPTY_TAG = CompoundTagBuilder.builder().buildRootTag();
    private static final byte[] EMPTY_LEVEL_CHUNK_DATA;

    static {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(new byte[258]); // Biomes + Border Size + Extra Data Size

            try (NBTOutputStream stream = NbtUtils.createNetworkWriter(outputStream)) {
                stream.write(EMPTY_TAG);
            }

            EMPTY_LEVEL_CHUNK_DATA = outputStream.toByteArray();
        }catch (IOException e) {
            throw new AssertionError("Unable to generate empty level chunk data");
        }
    }

    public static void start() {
        addLoginPackets();
        addChatPackets();
        addTitlePackets();
        addTimePackets();
        addEntityPackets();
        addNotifyPackets();
    }

    private static void addLoginPackets() {
        Registry.add(ServerJoinGamePacket.class, (packet, session) -> {
            AdventureSettingsPacket bedrockPacket = new AdventureSettingsPacket();
            bedrockPacket.setUniqueEntityId(packet.getEntityId());
            session.getUpstream().sendPacketImmediately(bedrockPacket);

            Vector3f pos = new Vector3f(0, 0, 0);
            int chunkX = pos.getFloorX() >> 4;
            int chunkZ = pos.getFloorZ() >> 4;
            for (int x = -3; x < 3; x++) {
                for (int z = -3; z < 3; z++) {
                    LevelChunkPacket data = new LevelChunkPacket();
                    data.setChunkX(chunkX + x);
                    data.setChunkZ(chunkZ + z);
                    data.setSubChunksLength(0);

                    data.setData(EMPTY_LEVEL_CHUNK_DATA);

                    session.getUpstream().sendPacketImmediately(data);

                }
            }
        });
    }

    private static void addChatPackets() {
        Registry.add(ServerChatPacket.class, (packet, session) -> {
            TextPacket textPacket = new TextPacket();
            textPacket.setPlatformChatId("");
            textPacket.setSourceName("");
            textPacket.setXuid(session.getAuthenticationData().getXboxUUID());
            switch (packet.getType()) {
                case CHAT:
                    textPacket.setType(TextPacket.Type.CHAT);
                case SYSTEM:
                    textPacket.setType(TextPacket.Type.SYSTEM);
                case NOTIFICATION:
                    textPacket.setType(TextPacket.Type.TIP);
                default:
                    textPacket.setType(TextPacket.Type.RAW);
            }

            if (packet.getMessage() instanceof TranslationMessage) {
                textPacket.setType(TextPacket.Type.TRANSLATION);
                textPacket.setNeedsTranslation(true);
                textPacket.setParameters(MessageUtils.getTranslationParams(((TranslationMessage) packet.getMessage()).getTranslationParams()));
                textPacket.setMessage(MessageUtils.getBedrockMessage(packet.getMessage()));
            } else {
                textPacket.setNeedsTranslation(false);
                textPacket.setMessage(MessageUtils.getBedrockMessage(packet.getMessage()));
            }

            session.getUpstream().sendPacket(textPacket);
        });
    }

    public static void addTitlePackets() {
        Registry.add(ServerTitlePacket.class, (packet, session) -> {
            SetTitlePacket titlePacket = new SetTitlePacket();

            switch (packet.getAction()) {
                case TITLE:
                    titlePacket.setType(SetTitlePacket.Type.SET_TITLE);
                    titlePacket.setText(packet.getTitle().getFullText());
                    break;
                case SUBTITLE:
                    titlePacket.setType(SetTitlePacket.Type.SET_SUBTITLE);
                    titlePacket.setText(packet.getSubtitle().getFullText());
                    break;
                case CLEAR:
                case RESET:
                    titlePacket.setType(SetTitlePacket.Type.RESET_TITLE);
                    titlePacket.setText("");
                    break;
                case ACTION_BAR:
                    titlePacket.setType(SetTitlePacket.Type.SET_ACTIONBAR_MESSAGE);
                    titlePacket.setText(packet.getActionBar().getFullText());
                    break;
            }

            titlePacket.setFadeInTime(packet.getFadeIn());
            titlePacket.setFadeOutTime(packet.getFadeOut());
            titlePacket.setStayTime(packet.getStay());

            session.getUpstream().sendPacket(titlePacket);
        });
    }

    public static void addTimePackets() {
        Registry.add(ServerUpdateTimePacket.class, (packet, session) -> {
            SetTimePacket setTimePacket = new SetTimePacket();
            setTimePacket.setTime((int) Math.abs(packet.getTime()));

            session.getUpstream().sendPacket(setTimePacket);
        });
    }

    public static void addEntityPackets() {
        Registry.add(ServerEntityPositionPacket.class, (packet, session) -> {
            MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
            moveEntityPacket.setRuntimeEntityId(packet.getEntityId());
            moveEntityPacket.setPosition(new Vector3f(packet.getMovementX(), packet.getMovementY(), packet.getMovementZ()));
            moveEntityPacket.setRotation(new Vector3f(packet.getMovementX(), packet.getMovementY(), packet.getMovementZ()));
            moveEntityPacket.setOnGround(packet.isOnGround());
            moveEntityPacket.setTeleported(false);

            session.getUpstream().sendPacket(moveEntityPacket);
        });

        Registry.add(ServerEntityPositionRotationPacket.class, (packet, session) -> {
            MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
            moveEntityPacket.setRuntimeEntityId(packet.getEntityId());
            moveEntityPacket.setPosition(new Vector3f(packet.getMovementX(), packet.getMovementY(), packet.getMovementZ()));
            moveEntityPacket.setRotation(new Vector3f(packet.getMovementX(), packet.getMovementY(), packet.getMovementZ()));
            moveEntityPacket.setOnGround(true);
            moveEntityPacket.setTeleported(false);

            session.getUpstream().sendPacket(moveEntityPacket);
        });

        Registry.add(ServerEntityTeleportPacket.class, (packet, session) -> {
            MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
            moveEntityPacket.setRuntimeEntityId(packet.getEntityId());
            moveEntityPacket.setPosition(new Vector3f(packet.getX(), packet.getY(), packet.getZ()));
            moveEntityPacket.setRotation(new Vector3f(packet.getX(), packet.getY(), packet.getZ()));
            moveEntityPacket.setOnGround(packet.isOnGround());
            moveEntityPacket.setTeleported(true);

            session.getUpstream().sendPacket(moveEntityPacket);
        });

        Registry.add(ServerEntityVelocityPacket.class, (packet, session) -> {
            SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
            entityMotionPacket.setRuntimeEntityId(packet.getEntityId());
            entityMotionPacket.setMotion(new Vector3f(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ()));

            session.getUpstream().sendPacket(entityMotionPacket);
        });
    }

    public static void addNotifyPackets() {
        Registry.add(ServerNotifyClientPacket.class, (packet, session) -> {
            switch (packet.getNotification()) {
                case START_RAIN:
                    LevelEventPacket startRainPacket = new LevelEventPacket();
                    startRainPacket.setEvent(LevelEventPacket.Event.START_RAIN);
                    startRainPacket.setData(ThreadLocalRandom.current().nextInt(50000) + 10000);
                    startRainPacket.setPosition(new Vector3f(0, 0, 0));

                    session.getUpstream().sendPacket(startRainPacket);
                    break;
                case STOP_RAIN:
                    LevelEventPacket stopRainPacket = new LevelEventPacket();
                    stopRainPacket.setEvent(LevelEventPacket.Event.STOP_RAIN);
                    stopRainPacket.setData(ThreadLocalRandom.current().nextInt(50000) + 10000);
                    stopRainPacket.setPosition(new Vector3f(0, 0, 0));

                    session.getUpstream().sendPacket(stopRainPacket);
                    break;
                case ENTER_CREDITS:
                    // ShowCreditsPacket showCreditsPacket = new ShowCreditsPacket();
                    // showCreditsPacket.setStatus(ShowCreditsPacket.Status.START_CREDITS);
                    // showCreditsPacket.setRuntimeEntityId(runtimeEntityId);
                    // session.getUpstream().sendPacket(showCreditsPacket);
                    break;
                default:
                    break;
            }
        });
    }
}
