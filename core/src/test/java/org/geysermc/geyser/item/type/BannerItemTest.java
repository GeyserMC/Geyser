package org.geysermc.geyser.item.type;

import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.item.components.Rarity;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.BannerPatternLayer;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BannerItemTest {
    private BannerItem bannerItem;

    @BeforeEach
    void setUp() {
        Block block = mock(Block.class);
        Key key = MinecraftKey.key("white_banner");
        when(block.javaIdentifier()).thenReturn(key);
        bannerItem = new BannerItem(Item.builder(), block);
    }

    @Test
    void testBannerItemDefaultRarity() {
        DataComponents components = new DataComponents(new HashMap<>());

        assertEquals(Rarity.COMMON, bannerItem.getRarity(null, components));
    }

    @Test
    void testBannerItemWithNonOminousPatternsRarity() {
        DataComponents components = new DataComponents(new HashMap<>());
        components.put(DataComponentTypes.BANNER_PATTERNS, Collections.<BannerPatternLayer>emptyList());

        assertEquals(Rarity.COMMON, bannerItem.getRarity(null, components));
    }

    @Test
    void testBannerItemWithExplicitRarity() {
        DataComponents components = new DataComponents(new HashMap<>());
        components.put(DataComponentTypes.RARITY, Rarity.UNCOMMON.ordinal());

        assertEquals(Rarity.UNCOMMON, bannerItem.getRarity(null, components));
    }

    @Test
    void testBannerItemNullComponents() {
        assertEquals(Rarity.COMMON, bannerItem.getRarity(null, null));
    }

    @Test
    void testItemRarityWithExplicitComponent() {
        Item item = new Item("minecraft:stick", Item.builder());
        DataComponents components = new DataComponents(new HashMap<>());
        components.put(DataComponentTypes.RARITY, Rarity.RARE.ordinal());

        assertEquals(Rarity.RARE, item.getRarity(null, components));
    }

    @Test
    void testItemNullComponents() {
        Item item = new Item("minecraft:stick", Item.builder());
        assertEquals(Rarity.COMMON, item.getRarity(null, null));
    }

    @Test
    void testIsOminousBlockEntityPatterns() {
        List<NbtMap> ominousPatterns = List.of(
                NbtMap.builder().putString("pattern", "minecraft:rhombus").putString("color", "cyan").build(),
                NbtMap.builder().putString("pattern", "minecraft:stripe_bottom").putString("color", "light_gray").build(),
                NbtMap.builder().putString("pattern", "minecraft:stripe_center").putString("color", "gray").build(),
                NbtMap.builder().putString("pattern", "minecraft:border").putString("color", "light_gray").build(),
                NbtMap.builder().putString("pattern", "minecraft:stripe_middle").putString("color", "black").build(),
                NbtMap.builder().putString("pattern", "minecraft:half_horizontal").putString("color", "light_gray").build(),
                NbtMap.builder().putString("pattern", "minecraft:circle").putString("color", "light_gray").build(),
                NbtMap.builder().putString("pattern", "minecraft:border").putString("color", "black").build()
        );

        assertTrue(BannerItem.isOminous(ominousPatterns));

        List<NbtMap> nonOminousPatterns = new ArrayList<>(ominousPatterns);
        nonOminousPatterns.removeLast();
        assertFalse(BannerItem.isOminous(nonOminousPatterns));
    }
}
