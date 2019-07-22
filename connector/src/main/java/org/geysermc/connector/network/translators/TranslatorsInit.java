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

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.github.steveice10.mc.protocol.data.game.scoreboard.ObjectiveAction;
import com.github.steveice10.mc.protocol.data.message.ChatFormat;
import com.github.steveice10.mc.protocol.data.message.TranslationMessage;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerTitlePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerDisplayScoreboardPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerUpdateScorePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTOutputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.GamePublishSetting;
import com.nukkitx.protocol.bedrock.data.GameRule;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.network.session.cache.ScoreboardCache;
import org.geysermc.connector.network.translators.scoreboard.Scoreboard;
import org.geysermc.connector.network.translators.scoreboard.ScoreboardObjective;
import org.geysermc.connector.utils.MessageUtils;
import org.geysermc.connector.utils.Toolbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
        addScoreboardPackets();
    }

    private static void addLoginPackets() {
        Registry.add(ServerJoinGamePacket.class, (packet, session) -> {
            AdventureSettingsPacket bedrockPacket = new AdventureSettingsPacket();

            bedrockPacket.setUniqueEntityId(packet.getEntityId());

            session.getUpstream().sendPacketImmediately(bedrockPacket);

            StartGamePacket startGamePacket = new StartGamePacket();
            startGamePacket.setUniqueEntityId(packet.getEntityId());
            startGamePacket.setRuntimeEntityId(packet.getEntityId());
            startGamePacket.setPlayerGamemode(packet.getGameMode().ordinal());
            startGamePacket.setPlayerPosition(new Vector3f(0, 0, 0));
            startGamePacket.setRotation(new Vector2f(1, 1));

            startGamePacket.setSeed(1111);
            startGamePacket.setDimensionId(0);
            startGamePacket.setGeneratorId(0);
            startGamePacket.setLevelGamemode(packet.getGameMode().ordinal());
            startGamePacket.setDifficulty(1);
            startGamePacket.setDefaultSpawn(new Vector3i(0, 0, 0));
            startGamePacket.setAcheivementsDisabled(true);
            startGamePacket.setTime(0);
            startGamePacket.setEduLevel(false);
            startGamePacket.setEduFeaturesEnabled(false);
            startGamePacket.setRainLevel(0);
            startGamePacket.setLightningLevel(0);
            startGamePacket.setMultiplayerGame(true);
            startGamePacket.setBroadcastingToLan(true);
            startGamePacket.getGamerules().add(new GameRule<>("showcoordinates", true));
            startGamePacket.setPlatformBroadcastMode(GamePublishSetting.PUBLIC);
            startGamePacket.setXblBroadcastMode(GamePublishSetting.PUBLIC);
            startGamePacket.setCommandsEnabled(true);
            startGamePacket.setTexturePacksRequired(false);
            startGamePacket.setBonusChestEnabled(false);
            startGamePacket.setStartingWithMap(false);
            startGamePacket.setTrustingPlayers(true);
            startGamePacket.setDefaultPlayerPermission(1);
            startGamePacket.setServerChunkTickRange(4);
            startGamePacket.setBehaviorPackLocked(false);
            startGamePacket.setResourcePackLocked(false);
            startGamePacket.setFromLockedWorldTemplate(false);
            startGamePacket.setUsingMsaGamertagsOnly(false);
            startGamePacket.setFromWorldTemplate(false);
            startGamePacket.setWorldTemplateOptionLocked(false);

            startGamePacket.setLevelId("oerjhii");
            startGamePacket.setWorldName("world");
            startGamePacket.setPremiumWorldTemplateId("00000000-0000-0000-0000-000000000000");
            startGamePacket.setCurrentTick(0);
            startGamePacket.setEnchantmentSeed(0);
            startGamePacket.setMultiplayerCorrelationId("");
            startGamePacket.setCachedPalette(Toolbox.CACHED_PALLETE);
            startGamePacket.setItemEntries(Toolbox.ITEMS);

            session.getUpstream().sendPacket(startGamePacket);

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

            PlayStatusPacket playStatusPacket = new PlayStatusPacket();
            playStatusPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
            session.getUpstream().sendPacket(playStatusPacket);
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

    public static void addScoreboardPackets() {
        Registry.add(ServerDisplayScoreboardPacket.class, (packet, session) -> {
            try {
                ScoreboardCache cache = session.getScoreboardCache();
                Scoreboard scoreboard = new Scoreboard(session);

                /*
                if (cache.getScoreboard() != null)
                    cache.setScoreboard(scoreboard);
                */
                System.out.println("added scoreboard " + packet.getScoreboardName());
                // scoreboard.onUpdate();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Registry.add(ServerScoreboardObjectivePacket.class, (packet, session) -> {
            try {
                ScoreboardCache cache = session.getScoreboardCache();
                Scoreboard scoreboard = new Scoreboard(session);
                if (cache.getScoreboard() != null)
                    scoreboard = cache.getScoreboard();


                System.out.println("new objective registered with " + packet.getName());
                if (packet.getAction() == ObjectiveAction.ADD) {
                    ScoreboardObjective objective = scoreboard.registerNewObjective(packet.getName());
                    objective.setDisplaySlot(ScoreboardObjective.DisplaySlot.SIDEBAR);
                    objective.setDisplayName(MessageUtils.getBedrockMessage(packet.getDisplayName()));
                    scoreboard.onUpdate();
                } else if (packet.getAction() == ObjectiveAction.UPDATE) {
                    ScoreboardObjective objective = scoreboard.getObjective(packet.getName());
                    objective.setDisplayName(MessageUtils.getBedrockMessage(packet.getDisplayName()));
                    scoreboard.onUpdate();
                } else {
                    scoreboard.unregisterObjective(packet.getName());
                }
                cache.setScoreboard(scoreboard);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Registry.add(ServerTeamPacket.class, (packet, session) -> {
            try {
                ScoreboardCache cache = session.getScoreboardCache();
                Scoreboard scoreboard = new Scoreboard(session);
                if (cache.getScoreboard() != null)
                    scoreboard = cache.getScoreboard();

                ScoreboardObjective objective = scoreboard.getObjective();
                if (objective == null) {
                    return;
                }

                System.out.println("Team name: " + packet.getTeamName());
                // System.out.println("Team Name: " + packet.getTeamName() + " displ: " + packet.getDisplayName() + " <-> objective team = " + packet.getTeamName());
                String scoreboardText = MessageUtils.getBedrockMessage(packet.getPrefix()) + MessageUtils.getBedrockMessage(packet.getSuffix());

                // System.out.println("scoreboard text: " + scoreboardText);
                switch (packet.getAction()) {
                    case REMOVE:
                    case REMOVE_PLAYER:

                        objective.registerScore(packet.getTeamName(), scoreboardText, Integer.parseInt(packet.getTeamName()), SetScorePacket.Action.REMOVE);
                        objective.setScoreText(packet.getTeamName(), scoreboardText);
                        break;
                    case UPDATE:
                        objective.setScoreText(packet.getTeamName(), scoreboardText);
                    case ADD_PLAYER:
                    case CREATE:
                        objective.registerScore(packet.getTeamName(), scoreboardText, Integer.parseInt(packet.getTeamName()), SetScorePacket.Action.SET);
                        objective.setScoreText(packet.getTeamName(), scoreboardText);
                        break;
                }

                cache.setScoreboard(scoreboard);
                scoreboard.onUpdate();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Registry.add(ServerUpdateScorePacket.class, (packet, session) -> {
            try {
                ScoreboardCache cache = session.getScoreboardCache();
                Scoreboard scoreboard = new Scoreboard(session);
                if (cache.getScoreboard() != null)
                    scoreboard = cache.getScoreboard();

                ScoreboardObjective objective = scoreboard.getObjective(packet.getObjective());
                if (objective == null) {
                    objective = scoreboard.registerNewObjective(packet.getObjective());
                }

                System.out.println(packet.getEntry() + " <-> objective = " + packet.getObjective() + " val " + packet.getValue());
                switch (packet.getAction()) {
                    case REMOVE:
                        objective.registerScore(packet.getEntry(), packet.getEntry(), packet.getValue(), SetScorePacket.Action.REMOVE);
                        objective.setScoreText(packet.getEntry(), packet.getEntry());
                        break;
                    case ADD_OR_UPDATE:
                        objective.registerScore(packet.getEntry(), packet.getEntry(), packet.getValue(), SetScorePacket.Action.SET);
                        objective.setScoreText(packet.getEntry(), packet.getEntry());
                        break;
                }

                cache.setScoreboard(scoreboard);
                scoreboard.onUpdate();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /*
    public static void addScoreboardPackets() {
        Registry.add(ServerDisplayScoreboardPacket.class, (packet, session) -> {
            SetDisplayObjectivePacket objectivePacket = new SetDisplayObjectivePacket();
            objectivePacket.setCriteria("dummy");
            objectivePacket.setDisplaySlot("sidebar");
            objectivePacket.setDisplayName(packet.getScoreboardName());
            objectivePacket.setSortOrder(1);
            objectivePacket.setObjectiveId(UUID.randomUUID().toString()); // uhh

            session.getUpstream().sendPacket(objectivePacket);

            System.out.println("added scoreboard " + packet.getScoreboardName());
        });

        Registry.add(ServerScoreboardObjectivePacket.class, (packet, session) -> {
            if (packet.getAction() == ObjectiveAction.REMOVE) {
                RemoveObjectivePacket objectivePacket = new RemoveObjectivePacket();
                objectivePacket.setObjectiveId(packet.getDisplayName().getFullText());

                session.getUpstream().sendPacket(objectivePacket);
            } else {
                SetDisplayObjectivePacket objectivePacket = new SetDisplayObjectivePacket();
                objectivePacket.setCriteria("dummy");
                objectivePacket.setDisplaySlot("sidebar");
                objectivePacket.setDisplayName(packet.getDisplayName().getFullText());
                objectivePacket.setSortOrder(1);
                objectivePacket.setObjectiveId(packet.getName()); // uhh

                session.getUpstream().sendPacket(objectivePacket);
            }

            System.out.println("new objective registered with " + packet.getName());
        });

        Registry.add(ServerUpdateScorePacket.class, (packet, session) -> {
            if (packet.getAction() == ScoreboardAction.ADD_OR_UPDATE) {
                SetDisplayObjectivePacket objectivePacket = new SetDisplayObjectivePacket();
                objectivePacket.setObjectiveId(packet.getObjective());
                objectivePacket.setDisplaySlot("sidebar");
                objectivePacket.setCriteria("dummy");
                objectivePacket.setDisplayName(packet.getEntry());
                objectivePacket.setSortOrder(1);

                session.getUpstream().sendPacket(objectivePacket);
            } else {
                RemoveObjectivePacket objectivePacket = new RemoveObjectivePacket();
                objectivePacket.setObjectiveId(packet.getObjective());

                session.getUpstream().sendPacket(objectivePacket);
            }

            System.out.println(packet.getEntry() + " <-> objective = " + packet.getObjective() + " val " + packet.getValue());
        });
    }
     */
}
