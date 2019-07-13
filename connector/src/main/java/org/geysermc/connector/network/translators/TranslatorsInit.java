package org.geysermc.connector.network.translators;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import com.nukkitx.protocol.bedrock.data.GamePublishSetting;
import com.nukkitx.protocol.bedrock.data.GameRule;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.v340.serializer.FullChunkDataSerializer_v340;
import com.nukkitx.protocol.bedrock.v340.serializer.ResourcePackChunkDataSerializer_v340;
import org.geysermc.connector.utils.Toolbox;

public class TranslatorsInit {
    public static void start() {
        addLoginPackets();
    }

    private static void addLoginPackets() {
        Registry.add(ServerJoinGamePacket.class, (x, y) -> {
            AdventureSettingsPacket bedrockPacket = new AdventureSettingsPacket();

            bedrockPacket.setUniqueEntityId(x.getEntityId());

            y.getUpstream().sendPacketImmediately(bedrockPacket);

            System.out.println(y.getUpstream().isClosed());

            StartGamePacket startGamePacket = new StartGamePacket();
            startGamePacket.setUniqueEntityId(x.getEntityId());
            startGamePacket.setRuntimeEntityId(x.getEntityId());
            startGamePacket.setPlayerGamemode(x.getGameMode().ordinal());
            startGamePacket.setPlayerPosition(new Vector3f(0, 0, 0));
            startGamePacket.setRotation(new Vector2f(1, 1));

            startGamePacket.setSeed(1111);
            startGamePacket.setDimensionId(0);
            startGamePacket.setGeneratorId(0);
            startGamePacket.setLevelGamemode(x.getGameMode().ordinal());
            startGamePacket.setDifficulty(1);
            startGamePacket.setDefaultSpawn(new Vector3i(0, 0, 0));
            startGamePacket.setAcheivementsDisabled(true);
            startGamePacket.setTime(1300);
            startGamePacket.setEduLevel(false);
            startGamePacket.setEduFeaturesEnabled(false);
            startGamePacket.setRainLevel(0);
            startGamePacket.setLightningLevel(0);
            startGamePacket.setMultiplayerGame(false);
            startGamePacket.setBroadcastingToLan(true);
            startGamePacket.getGamerules().add((new GameRule("showcoordinates", true)));
            startGamePacket.setPlatformBroadcastMode(GamePublishSetting.FRIENDS_OF_FRIENDS);
            startGamePacket.setXblBroadcastMode(GamePublishSetting.FRIENDS_OF_FRIENDS);
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
            startGamePacket.setCurrentTick(1);
            startGamePacket.setEnchantmentSeed(1);
            startGamePacket.setMultiplayerCorrelationId("");
            startGamePacket.setCachedPalette(Toolbox.CACHED_PALLETE);

            y.getUpstream().sendPacketImmediately(startGamePacket);

            System.out.println(y.getUpstream().isClosed());

            Vector3f pos = new Vector3f(0, 0, 0);

            int chunkX = pos.getFloorX() >> 4;

            int chunkZ = pos.getFloorX() >> 4;

            for (int x1 = -3; x1 < 3; x1++) {

                for (int z = -3; z < 3; z++) {

                    LevelChunkPacket data = new LevelChunkPacket();

                    data.setChunkX(chunkX + x1);

                    data.setChunkZ(chunkZ + z);

                    data.setData(new byte[0]);

                    y.getUpstream().sendPacketImmediately(data);

                    System.out.println(y.getUpstream().isClosed());

                }

            }

            PlayStatusPacket packet = new PlayStatusPacket();

            packet.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);

            y.getUpstream().sendPacket(packet);

            System.out.println(y.getUpstream().isClosed());
        });
    }
}
