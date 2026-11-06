/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.biome.custom;

import org.geysermc.geyser.api.biome.custom.CustomBiomeAppearance;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.biome.custom.CustomBiomePrecipitation;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class CustomBiomeDefinitionTest {

    @Test
    void validatesDefinitionInputs() {
        GeyserMockContext.mockContext(() -> {
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeDefinition.builder(Identifier.of("minecraft:plains")).build());
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeDefinition.builder(Identifier.of("my_datapack:cave/deep_caves")).build());
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeDefinition.builder(Identifier.of("geyser:auto_abc123")).build());

            assertThrows(IllegalArgumentException.class, () -> CustomBiomeDefinition.builder(Identifier.of("test:biome")).tag("Uppercase").build());
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeDefinition.builder(Identifier.of("test:biome")).tag("minecraft:cold").build());
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeDefinition.builder(Identifier.of("test:biome")).tag(":").build());
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeDefinition.builder(Identifier.of("test:biome")).tag("a:b:c").build());
            assertThrows(NullPointerException.class, () -> CustomBiomeDefinition.builder(Identifier.of("test:biome")).tag(null));

            // The catalogue is immutable after registration, which only holds for values
            // created through the API builders
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeDefinition.builder(Identifier.of("test:biome"))
                .appearance(mock(CustomBiomeAppearance.class)).build());

            CustomBiomeDefinition definition = CustomBiomeDefinition.builder(Identifier.of("test:biome"))
                .tag("cold").tag("animal").tag("monster")
                .build();
            // Tag order must not depend on insertion order
            assertEquals(List.of("animal", "cold", "monster"), List.copyOf(definition.tags()));
        });
    }

    @Test
    void validatesAppearance() {
        GeyserMockContext.mockContext(() -> {
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeAppearance.builder().build());
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeAppearance.builder().waterSurfaceOpacity(1.5F).build());
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeAppearance.builder().waterFogEndDistance(-1.0F).build());
            assertThrows(IllegalArgumentException.class, () -> CustomBiomePrecipitation.of(CustomBiomePrecipitation.Type.ASH, -0.5F));
            assertThrows(NullPointerException.class, () -> CustomBiomeAppearance.builder().skyColor(null));
            assertThrows(IllegalArgumentException.class, () -> CustomBiomeAppearance.builder()
                .precipitation(mock(CustomBiomePrecipitation.class)).build());

            // Vanilla basalt deltas use a white ash density of 2.0; it must be accepted
            assertEquals(2.0F, CustomBiomePrecipitation.of(CustomBiomePrecipitation.Type.WHITE_ASH, 2.0F).density());
            // A water fog color does not require an end distance
            assertEquals(new Color(0x050533), CustomBiomeAppearance.builder().waterFogColor(new Color(0x050533)).build().waterFogColor());

            // Appearance colors are RGB only, so alpha must not affect equality
            CustomBiomeAppearance opaque = CustomBiomeAppearance.builder().skyColor(new Color(0x78a7ff)).build();
            assertEquals(opaque, CustomBiomeAppearance.builder().skyColor(new Color(0x78, 0xa7, 0xff, 0x12)).build());
            assertEquals(new Color(0x40a7ff), CustomBiomeAppearance.builder()
                .skyColor(new Color(0x40, 0xa7, 0xff, 0x78)).build().skyColor());
        });
    }
}
