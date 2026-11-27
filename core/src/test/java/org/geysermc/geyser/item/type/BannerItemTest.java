package org.geysermc.geyser.item.type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.item.components.Rarity;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerItemTest {
    @Test
    void creativeOminousBannerRestoresRarityWithoutForcingNameColor() {
        GeyserMockContext.mockContext(() -> checkCreativeBanners());
    }

    private void checkCreativeBanners() {
        BannerItem banner = mock(BannerItem.class, CALLS_REAL_METHODS);
        GeyserSession session = mock(GeyserSession.class, RETURNS_DEEP_STUBS);
        when(session.locale()).thenReturn("en_us");
        DataComponents components = new DataComponents(new HashMap<>());

        banner.translateNbtToJava(session, NbtMap.builder().putInt("Type", 1).build(), components, null);

        assertEquals(Rarity.UNCOMMON.ordinal(), components.get(DataComponentTypes.RARITY));
        assertEquals(Component.translatable("block.minecraft.ominous_banner"), components.get(DataComponentTypes.ITEM_NAME));
        assertEquals(8, components.get(DataComponentTypes.BANNER_PATTERNS).size());

        ItemMapping mapping = mock(ItemMapping.class);
        when(mapping.getJavaItem()).thenReturn(banner);
        // Use literal text to keep this check independent of downloaded language files.
        components.put(DataComponentTypes.ITEM_NAME, Component.text("Ominous Banner"));
        assertEquals(ChatColor.RESET + ChatColor.YELLOW + "Ominous Banner",
                ItemTranslator.getCustomName(session, components, mapping, Rarity.UNCOMMON.getColor(), false, false));
        components.put(DataComponentTypes.ITEM_NAME, Component.text("Named Banner", NamedTextColor.RED));
        assertEquals(ChatColor.RESET + ChatColor.YELLOW + ChatColor.RED + "Named Banner",
                ItemTranslator.getCustomName(session, components, mapping, Rarity.UNCOMMON.getColor(), false, false));

        DataComponents ordinaryBanner = new DataComponents(new HashMap<>());
        banner.translateNbtToJava(session, NbtMap.EMPTY, ordinaryBanner, null);
        assertNull(ordinaryBanner.get(DataComponentTypes.RARITY));
        assertNull(ordinaryBanner.get(DataComponentTypes.ITEM_NAME));
    }
}
