package org.geysermc.geyser.item.hashing;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PotionContents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

class PotionContentsHashTest {
    @Test
    void presentSignedColorsAreIncludedInTheHash() {
        var encoder = new MinecraftHashEncoder(mock(JavaRegistryProvider.class));
        var hasher = DataComponentHashers.hasher(DataComponentTypes.POTION_CONTENTS);
        for (int color : new int[]{-1, Integer.MIN_VALUE, 0xff336699, 0}) {
            var contents = new PotionContents(-1, OptionalInt.of(color), List.of(), null);
            assertEquals(encoder.nbtMap(NbtMap.builder().putInt("custom_color", color).build()),
                hasher.hash(contents, encoder));
        }
    }

    @Test
    void absentColorDiffersFromPresentMinusOne() {
        var encoder = new MinecraftHashEncoder(mock(JavaRegistryProvider.class));
        var hasher = DataComponentHashers.hasher(DataComponentTypes.POTION_CONTENTS);
        var absent = new PotionContents(-1, -1, List.of(), null);
        var present = new PotionContents(-1, OptionalInt.of(-1), List.of(), null);
        assertEquals(encoder.nbtMap(NbtMap.EMPTY), hasher.hash(absent, encoder));
        assertNotEquals(hasher.hash(absent, encoder), hasher.hash(present, encoder));
    }
}
