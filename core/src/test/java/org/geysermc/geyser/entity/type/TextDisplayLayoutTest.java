/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 */
package org.geysermc.geyser.entity.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextDisplayLayoutTest {
    @Test
    void explicitJavaFixedBillboardKeepsItsRotation() {
        assertEquals(true, TextDisplayEntity.usesFixedRotation(DisplayBaseEntity.Billboard.FIXED));
    }

    @Test
    void javaCameraFacingBillboardModesAreNotLockedInWorldSpace() {
        // DebugText only exposes a fixed-rotation toggle, not the three
        // camera-facing constraints implemented in Java Edition.
        assertEquals(false, TextDisplayEntity.usesFixedRotation(DisplayBaseEntity.Billboard.VERTICAL));
        assertEquals(false, TextDisplayEntity.usesFixedRotation(DisplayBaseEntity.Billboard.HORIZONTAL));
        assertEquals(false, TextDisplayEntity.usesFixedRotation(DisplayBaseEntity.Billboard.CENTER));
    }

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
    void standaloneCustomFontGlyphUsesBedrockFontCorrection() {
        // VoxelFurniture's Help Box has a 1.5x text-display transform. Its
        // bitmap glyph needs a separate Bedrock font correction, not another
        // world-space translation or an adjustment to the whole text renderer.
        assertEquals(1.20f, TextDisplayEntity.adjustedGlyphScale("\uE200", 1.5f, 0.80f), 0.00001f);
        assertEquals(0.80f, TextDisplayEntity.adjustedGlyphScale("\uE200", 1.0f, 0.80f), 0.00001f);
    }

    @Test
    void customGlyphVerticalOffsetTracksJavaScaleButNotGlyphScaleCorrection() {
        // At 1.5x Java scale, -0.40 blocks per unit moves the Help Box
        // glyph down 0.60 world blocks, irrespective of the 0.80 font sizing.
        assertEquals(-0.60f, TextDisplayEntity.singleGlyphVerticalOffset("\uE200", 1.5f, -0.40f), 0.00001f);
        assertEquals(-0.40f, TextDisplayEntity.singleGlyphVerticalOffset("\uE200", 1.0f, -0.40f), 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset("\uE200", 1.0f, 0.0f), 0.00001f);
    }

    @Test
    void regularTextAndMixedGlyphsRetainOriginalVerticalAnchor() {
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset("SURVIVAL PORTAL", 1.5f, -0.40f), 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset("Title \uE200", 1.5f, -0.40f), 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset("\uE200\uE201", 1.5f, -0.40f), 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset("?", 1.5f, -0.40f), 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset(null, 1.5f, -0.40f), 0.00001f);
    }

    @Test
    void standardAndMixedTextNeverReceivesCustomGlyphCorrection() {
        assertEquals(1.5f, TextDisplayEntity.adjustedGlyphScale("SURVIVAL PORTAL", 1.5f, 0.80f), 0.00001f);
        assertEquals(1.5f, TextDisplayEntity.adjustedGlyphScale("Text \uE200", 1.5f, 0.80f), 0.00001f);
        assertEquals(1.5f, TextDisplayEntity.adjustedGlyphScale("\uE200\uE201", 1.5f, 0.80f), 0.00001f);
        assertEquals(1.5f, TextDisplayEntity.adjustedGlyphScale("?", 1.5f, 0.80f), 0.00001f);
        assertEquals(1.5f, TextDisplayEntity.adjustedGlyphScale("", 1.5f, 0.80f), 0.00001f);
        assertEquals(1.5f, TextDisplayEntity.adjustedGlyphScale(null, 1.5f, 0.80f), 0.00001f);
    }

    @Test
    void emptyParagraphsAndTrailingNewlinesRemainRepresented() {
        assertEquals("heading\n\u00a0\nbody\n\u00a0",
                TextDisplayEntity.preserveBlankLines("heading\n\nbody\n"));
        assertEquals("heading\n\u00a0\nbody",
                TextDisplayEntity.preserveBlankLines("heading\r\n\r\nbody"));
    }
}
