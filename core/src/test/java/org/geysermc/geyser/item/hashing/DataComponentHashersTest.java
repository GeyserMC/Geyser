package org.geysermc.geyser.item.hashing;

import com.google.common.hash.HashCode;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.auth.GameProfile.Property;
import org.geysermc.mcprotocollib.auth.GameProfile.TextureModel;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.TypedEntityData;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataComponentHashersTest {

    private static final UUID PROFILE_ID = UUID.fromString("ec1f7e3f-3aa4-4f73-8f3a-0d873d61c9b1");

    @Test
    void resolvableProfileHasherBuildsExpectedMap() {
        GameProfile profile = new GameProfile(PROFILE_ID, "Geyser");
        profile.setProperties(List.of(new Property("textures", "value", "signature")));
        ResolvableProfile resolvable = new ResolvableProfile(
            profile,
            MinecraftKey.key("geyser:test/body"),
            MinecraftKey.key("geyser:test/cape"),
            MinecraftKey.key("geyser:test/elytra"),
            TextureModel.SLIM,
            true
        );

        GeyserSession session = Mockito.mock(GeyserSession.class);
        MinecraftHashEncoder encoder = new MinecraftHashEncoder(session);

        HashCode actual = MinecraftHasher.RESOLVABLE_PROFILE.hash(resolvable, encoder);

        Map<HashCode, HashCode> expectedEntries = new HashMap<>();
        expectedEntries.put(encoder.string("profile"), MinecraftHasher.GAME_PROFILE.hash(profile, encoder));
        expectedEntries.put(encoder.string("body"), MinecraftHasher.KEY.hash(MinecraftKey.key("geyser:test/body"), encoder));
        expectedEntries.put(encoder.string("cape"), MinecraftHasher.KEY.hash(MinecraftKey.key("geyser:test/cape"), encoder));
        expectedEntries.put(encoder.string("elytra"), MinecraftHasher.KEY.hash(MinecraftKey.key("geyser:test/elytra"), encoder));
        expectedEntries.put(encoder.string("model"), MinecraftHasher.TEXTURE_MODEL.hash(TextureModel.SLIM, encoder));
        expectedEntries.put(encoder.string("dynamic"), MinecraftHasher.BOOL.hash(true, encoder));

        HashCode expected = encoder.map(expectedEntries);
        assertEquals(expected, actual);
    }

    @Test
    void resolvableProfileHasherSkipsDefaultDynamic() {
        GameProfile profile = new GameProfile(PROFILE_ID, "Static");
        ResolvableProfile resolvable = new ResolvableProfile(profile);

        GeyserSession session = Mockito.mock(GeyserSession.class);
        MinecraftHashEncoder encoder = new MinecraftHashEncoder(session);

        HashCode actual = MinecraftHasher.RESOLVABLE_PROFILE.hash(resolvable, encoder);

        Map<HashCode, HashCode> expectedEntries = new HashMap<>();
        expectedEntries.put(encoder.string("profile"), MinecraftHasher.GAME_PROFILE.hash(profile, encoder));

        HashCode expected = encoder.map(expectedEntries);
        assertEquals(expected, actual);
    }

    @Test
    void entityDataHasherEncodesTypeAndTag() {
        testTypedEntityDataHasher(MinecraftHasher.INT.cast(EntityType::ordinal), EntityType.ZOMBIE, MinecraftKey.key("geyser:zombie"));
    }

    @Test
    void blockEntityDataHasherEncodesTypeAndTag() {
        testTypedEntityDataHasher(MinecraftHasher.INT.cast(BlockEntityType::ordinal), BlockEntityType.SKULL, MinecraftKey.key("geyser:skull"));
    }

    private static <Type> void testTypedEntityDataHasher(MinecraftHasher<Type> typeHasher,
                                                         Type type,
                                                         Key tagKey) {
        NbtMap tag = NbtMap.builder()
            .putString("key", tagKey.asString())
            .build();
        TypedEntityData<Type> data = new TypedEntityData<>(type, tag);

        GeyserSession session = Mockito.mock(GeyserSession.class);
        MinecraftHashEncoder encoder = new MinecraftHashEncoder(session);

        MinecraftHasher<TypedEntityData<Type>> hasher = MinecraftHasher.mapBuilder(builder -> builder
            .accept("type", typeHasher, TypedEntityData::type)
            .accept("tag", MinecraftHasher.NBT_MAP, TypedEntityData::tag));
        HashCode actual = hasher.hash(data, encoder);

        Map<HashCode, HashCode> expectedEntries = new HashMap<>();
        expectedEntries.put(encoder.string("type"), typeHasher.hash(type, encoder));
        expectedEntries.put(encoder.string("tag"), MinecraftHasher.NBT_MAP.hash(tag, encoder));

        HashCode expected = encoder.map(expectedEntries);
        assertEquals(expected, actual);
    }
}
