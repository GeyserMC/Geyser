/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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
 */

package org.geysermc.geyser.translator.item;

import net.kyori.adventure.key.Key;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.registry.type.CustomBlockItemOverride;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CustomBlockItemTranslatorTest {
    @Test
    void resolvesModelSpecificBeforeBase() {
        CustomBlockData modelSpecific = mock(CustomBlockData.class);
        CustomBlockData baseFallback = mock(CustomBlockData.class);
        var overrides = Map.of(
                new CustomBlockItemOverride("minecraft:paper", Key.key("example:test_note_block")), modelSpecific,
                CustomBlockItemOverride.base("minecraft:paper"), baseFallback);

        // Model-specific match takes priority
        assertSame(modelSpecific, CustomBlockItemTranslator.resolve(
                overrides, "minecraft:paper", Key.key("example:test_note_block")));
        // Different model falls back to base
        assertSame(baseFallback, CustomBlockItemTranslator.resolve(
                overrides, "minecraft:paper", Key.key("example:other")));
        // Null model also falls back to base
        assertSame(baseFallback, CustomBlockItemTranslator.resolve(
                overrides, "minecraft:paper", null));
        // Wrong java identifier
        assertNull(CustomBlockItemTranslator.resolve(
                overrides, "minecraft:note_block", Key.key("example:test_note_block")));
    }

    @Test
    void baseOverrideOnlyMatchesBaseKey() {
        CustomBlockData block = mock(CustomBlockData.class);
        var overrides = Map.of(CustomBlockItemOverride.base("minecraft:paper"), block);

        assertSame(block, CustomBlockItemTranslator.resolve(
                overrides, "minecraft:paper", null));
        assertNull(CustomBlockItemTranslator.resolve(
                overrides, "minecraft:note_block", null));
    }

    @Test
    void emptyRegistryReturnsNull() {
        var overrides = Map.<CustomBlockItemOverride, CustomBlockData>of();
        assertNull(CustomBlockItemTranslator.resolve(
                overrides, "minecraft:paper", Key.key("example:test")));
        assertNull(CustomBlockItemTranslator.resolve(
                overrides, "minecraft:paper", null));
    }

    @Test
    void baseFactoryKeyEqualsDirectConstructor() {
        var fromFactory = CustomBlockItemOverride.base("minecraft:paper");
        var fromConstructor = new CustomBlockItemOverride("minecraft:paper", null);
        assertEquals(fromFactory, fromConstructor,
                "base() must produce equal key to explicit (identifier, null)");
        assertEquals(fromFactory.hashCode(), fromConstructor.hashCode());
    }

    @Test
    void rejectsNullJavaIdentifier() {
        assertThrows(NullPointerException.class, () ->
                new CustomBlockItemOverride(null, null));
    }
}
