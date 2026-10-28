/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 */
package org.geysermc.geyser.entity.type;

import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextDisplayLayoutTest {
    @Test
    void textDisplayFlagsMatchJavaAndRestoreDepthTesting() {
        assertEquals(0x01, TextDisplayEntity.STYLE_FLAG_SHADOW);
        assertEquals(0x02, TextDisplayEntity.STYLE_FLAG_SEE_THROUGH);
        assertEquals(0x04, TextDisplayEntity.STYLE_FLAG_DEFAULT_BACKGROUND);

        // Default background must not accidentally make the text see-through.
        assertEquals(false, TextDisplayEntity.hasStyleFlag((byte) 0x00, TextDisplayEntity.STYLE_FLAG_SEE_THROUGH));
        assertEquals(false, TextDisplayEntity.hasStyleFlag((byte) 0x01, TextDisplayEntity.STYLE_FLAG_SEE_THROUGH));
        assertEquals(false, TextDisplayEntity.hasStyleFlag((byte) 0x04, TextDisplayEntity.STYLE_FLAG_SEE_THROUGH));

        // Only Java's explicit see_through bit disables Bedrock depth testing.
        assertEquals(true, TextDisplayEntity.hasStyleFlag((byte) 0x02, TextDisplayEntity.STYLE_FLAG_SEE_THROUGH));
        assertEquals(true, TextDisplayEntity.hasStyleFlag((byte) 0x06, TextDisplayEntity.STYLE_FLAG_SEE_THROUGH));

        // The other two flags remain independent.
        assertEquals(true, TextDisplayEntity.hasStyleFlag((byte) 0x01, TextDisplayEntity.STYLE_FLAG_SHADOW));
        assertEquals(false, TextDisplayEntity.hasStyleFlag((byte) 0x02, TextDisplayEntity.STYLE_FLAG_SHADOW));
        assertEquals(true, TextDisplayEntity.hasStyleFlag((byte) 0x04, TextDisplayEntity.STYLE_FLAG_DEFAULT_BACKGROUND));
        assertEquals(false, TextDisplayEntity.hasStyleFlag((byte) 0x02, TextDisplayEntity.STYLE_FLAG_DEFAULT_BACKGROUND));
    }

    @Test
    void seeThroughControlsBedrockDepthTest() {
        for (int value = 0; value < 32; value++) {
            boolean seeThrough = TextDisplayEntity.hasStyleFlag(
                    (byte) value, TextDisplayEntity.STYLE_FLAG_SEE_THROUGH);
            DebugText shape = new DebugText();
            TextDisplayEntity.applySeeThroughToBedrock(shape, seeThrough);
            assertEquals(!seeThrough, shape.isDepthTest());
        }
    }

    @Test
    void bedrockAnchorAccountsForJavaLineHeight() {
        // Java renders 9px glyphs and 1px line gap, at 40px/block.
        float oneLine = TextDisplayEntity.calculateBedrockVerticalOffset(1, 1.0f);
        float threeLines = TextDisplayEntity.calculateBedrockVerticalOffset(3, 1.0f);
        assertEquals(0.25f, threeLines - oneLine, 0.00001f);
    }

    @Test
    void calibratedDefaultVerticalAnchorOffsetsJavaTextHeight() {
        // One Java line is 9 pixels tall; at 1x the centre is 9 / 40 / 2
        // blocks above its bottom pivot, plus the measured -0.65 offset.
        assertEquals(-0.5375f, TextDisplayEntity.calculateBedrockVerticalOffset(1, 1.0f), 0.00001f);
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
        // At 1.5x Java scale, -0.25 blocks per unit moves the Help Box
        // glyph down 0.375 world blocks, irrespective of the 0.80 font sizing.
        assertEquals(-0.375f, TextDisplayEntity.singleGlyphVerticalOffset("\uE200", 1.5f, -0.25f), 0.00001f);
        assertEquals(-0.25f, TextDisplayEntity.singleGlyphVerticalOffset("\uE200", 1.0f, -0.25f), 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset("\uE200", 1.0f, 0.0f), 0.00001f);
    }

    @Test
    void regularTextAndMixedGlyphsRetainOriginalVerticalAnchor() {
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset("SURVIVAL PORTAL", 1.5f, -0.25f), 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset("Title \uE200", 1.5f, -0.25f), 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset("\uE200\uE201", 1.5f, -0.25f), 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset("?", 1.5f, -0.25f), 0.00001f);
        assertEquals(0.0f, TextDisplayEntity.singleGlyphVerticalOffset(null, 1.5f, -0.25f), 0.00001f);
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
