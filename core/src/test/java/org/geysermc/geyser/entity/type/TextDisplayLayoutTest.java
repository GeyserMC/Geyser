/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 */
package org.geysermc.geyser.entity.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextDisplayLayoutTest {
    @Test
    void bedrockAnchorAccountsForJavaLineHeight() {
        // Java renders 9px glyphs and 1px line gap, at 40px/block.
        float oneLine = TextDisplayEntity.calculateBedrockVerticalOffset(1, 1.0f);
        float threeLines = TextDisplayEntity.calculateBedrockVerticalOffset(3, 1.0f);
        assertEquals(0.25f, threeLines - oneLine, 0.00001f);
    }

    @Test
    void fontScaleChangesOnlyTextHeightNotTheBedrockAnchor() {
        // Five Java lines occupy 49px at 40px/block; pivot to centre is half that height.
        float full = TextDisplayEntity.calculateBedrockVerticalOffset(5, 1.0f);
        float half = TextDisplayEntity.calculateBedrockVerticalOffset(5, 0.5f);
        float oneAndHalf = TextDisplayEntity.calculateBedrockVerticalOffset(5, 1.5f);
        assertEquals(49.0f / 40.0f / 4.0f, full - half, 0.00001f);
        assertEquals(49.0f / 40.0f / 4.0f, oneAndHalf - full, 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.calculateBedrockVerticalOffset(0, 1.0f), 0.00001f);
    }

    @Test
    void oneLineFurnitureGlyphPreservesFixedAttachmentBaseline() {
        // Text height changes with scale, but the fixed Bedrock anchor must not;
        // otherwise custom 1.5x furniture glyphs float above their display.
        float normal = TextDisplayEntity.calculateBedrockVerticalOffset(1, 1.0f);
        float scaled = TextDisplayEntity.calculateBedrockVerticalOffset(1, 1.5f);
        assertEquals(9.0f / 40.0f / 4.0f, scaled - normal, 0.00001f);
    }

    @Test
    void emptyParagraphsAndTrailingNewlinesRemainRepresented() {
        assertEquals("heading\n\u00a0\nbody\n\u00a0",
                TextDisplayEntity.preserveBlankLines("heading\n\nbody\n"));
        assertEquals("heading\n\u00a0\nbody",
                TextDisplayEntity.preserveBlankLines("heading\r\n\r\nbody"));
    }
}
