package org.geysermc.geyser.entity.type.player;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.WorldCache;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MannequinEntityTest {

    private static final EntityDefinition<MannequinEntity> MANNEQUIN_DEFINITION = new EntityDefinition<>(
        null,
        EntityType.MANNEQUIN,
        "minecraft:mannequin",
        0.6f,
        1.8f,
        0.0f,
        null,
        List.of()
    );

    private static final EntityDefinition<TestAvatarEntity> AVATAR_DEFINITION = new EntityDefinition<>(
        null,
        EntityType.PLAYER,
        "minecraft:player",
        0.6f,
        1.8f,
        0.0f,
        null,
        List.of()
    );

    private GeyserSession session;

    @BeforeEach
    void setUp() {
        session = mock(GeyserSession.class);
        when(session.locale()).thenReturn("en_us");

        WorldCache worldCache = mock(WorldCache.class);
        Scoreboard scoreboard = mock(Scoreboard.class);
        when(worldCache.getScoreboard()).thenReturn(scoreboard);
        when(session.getWorldCache()).thenReturn(worldCache);
        when(scoreboard.getTeamFor(any())).thenReturn(null);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(session).ensureInEventLoop(any());
    }

    @Test
    void avatarEntityIgnoresDisplayNameMetadata() {
        TestAvatarEntity avatar = new TestAvatarEntity(session);
        EntityMetadata<Optional<Component>, MetadataType<Optional<Component>>> metadata =
            new ObjectEntityMetadata<>(0, MetadataTypes.OPTIONAL_COMPONENT, Optional.of(Component.text("Custom")));

        avatar.setDisplayName(metadata);

        assertEquals("TestUser", avatar.getUsername());
        assertEquals("TestUser", avatar.getNametag());
    }

    @Test
    void mannequinAppliesDisplayNameMetadata() {
        TestMannequinEntity mannequin = new TestMannequinEntity(session);
        EntityMetadata<Optional<Component>, MetadataType<Optional<Component>>> metadata =
            new ObjectEntityMetadata<>(0, MetadataTypes.OPTIONAL_COMPONENT, Optional.of(Component.text("Fancy")));

        mannequin.setDisplayName(metadata);

        assertEquals("Fancy", mannequin.getNametag());
    }

    @Test
    void mannequinUsesResolvedProfileNameWhenAvailable() throws Exception {
        TestMannequinEntity mannequin = new TestMannequinEntity(session);
        GameProfile resolvedProfile = new GameProfile(UUID.randomUUID(), "ResolvedName");
        ResolvableProfile resolvableProfile = new ResolvableProfile(
            new GameProfile(UUID.randomUUID(), "Partial"),
            null,
            null,
            null,
            null,
            false
        );

        invokeApplyProfile(mannequin, resolvableProfile, resolvedProfile);

        assertEquals("ResolvedName", mannequin.getUsername());
        assertEquals("ResolvedName", mannequin.getNametag());
        assertSame(resolvedProfile, mannequin.lastSkinProfile);
    }

    @Test
    void mannequinFallsBackToResolvableProfileName() throws Exception {
        TestMannequinEntity mannequin = new TestMannequinEntity(session);
        GameProfile unresolvedProfile = new GameProfile((UUID) null, null);
        ResolvableProfile resolvableProfile = new ResolvableProfile(
            new GameProfile(UUID.randomUUID(), "Partial"),
            null,
            null,
            null,
            null,
            false
        );

        invokeApplyProfile(mannequin, resolvableProfile, unresolvedProfile);

        assertEquals("Partial", mannequin.getUsername());
        assertEquals("Partial", mannequin.getNametag());
    }

    @Test
    void mannequinFallsBackToTranslatedNameWhenNoNamesProvided() throws Exception {
        TestMannequinEntity mannequin = new TestMannequinEntity(session);
        String originalName = mannequin.getUsername();

        ResolvableProfile emptyResolvableProfile = new ResolvableProfile(
            new GameProfile((UUID) null, null),
            null,
            null,
            null,
            null,
            false
        );
        GameProfile emptyProfile = new GameProfile((UUID) null, null);

        invokeApplyProfile(mannequin, emptyResolvableProfile, emptyProfile);

        assertEquals(originalName, mannequin.getUsername());
        assertEquals(originalName, mannequin.getNametag());
    }

    private static void invokeApplyProfile(MannequinEntity entity, ResolvableProfile resolvableProfile, GameProfile gameProfile) throws Exception {
        Method method = MannequinEntity.class.getDeclaredMethod("applyProfile", ResolvableProfile.class, GameProfile.class);
        method.setAccessible(true);
        method.invoke(entity, resolvableProfile, gameProfile);
    }

    private static class TestAvatarEntity extends AvatarEntity {
        TestAvatarEntity(GeyserSession session) {
            super(session, 1, 1L, UUID.randomUUID(), AVATAR_DEFINITION, Vector3f.from(0, 0, 0), Vector3f.from(0, 0, 0), 0f, 0f, 0f, "TestUser");
        }

        @Override
        public void setSkin(GameProfile profile, boolean cape, Runnable after) {
            if (after != null) {
                after.run();
            }
        }

        @Override
        public void setSkin(String texturesProperty, boolean cape, Runnable after) {
            if (after != null) {
                after.run();
            }
        }
    }

    private static class TestMannequinEntity extends MannequinEntity {
        private GameProfile lastSkinProfile;

        TestMannequinEntity(GeyserSession session) {
            super(session, 2, 2L, UUID.randomUUID(), MANNEQUIN_DEFINITION, Vector3f.from(0, 0, 0), Vector3f.from(0, 0, 0), 0f, 0f, 0f);
        }

        @Override
        public void setSkin(GameProfile profile, boolean cape, Runnable after) {
            this.lastSkinProfile = profile;
            if (after != null) {
                after.run();
            }
        }

        @Override
        public void setSkin(String texturesProperty, boolean cape, Runnable after) {
            if (after != null) {
                after.run();
            }
        }
    }
}
