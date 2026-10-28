/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.matrix.Matrix4f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector4f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugShape;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugText;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.DebugDrawerPacket;
import org.geysermc.geyser.entity.VanillaEntities;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.ColorUtils;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Optional;
import java.util.Set;

// Note: 1.19.4 requires that the billboard is set to something in order to show, on Java Edition
@Getter
public class TextDisplayEntity extends DisplayBaseEntity {

    /**
     * The height offset per line of text in a text display entity when rendered
     * as an armor stand nametag on Bedrock Edition. This value was empirically adjusted
     * to match Java Edition's multi-line text centering behavior.
     */
    private static final float LINE_HEIGHT_OFFSET = 0.1414f;

    /** Java displays render text at 40 pixels per block, with 10-pixel line spacing. */
    private static final float JAVA_TEXT_PIXEL_SCALE = 1.0f / 40.0f;

    /**
     * Bedrock's world-space text attachment origin differs from Java's.
     * Keep this correction constant regardless of text scale; only the
     * text block height is scaled separately.
     */
    private static final float BEDROCK_TEXT_ANCHOR_CORRECTION = -0.65f;

    /**
     * Correct Bedrock's larger rendering of standalone private-use font glyphs.
     * This scales only a single custom glyph, not normal text or mixed content.
     */
    private static final float BEDROCK_SINGLE_GLYPH_SCALE = 0.80f;

    /**
     * Additional downward offset for standalone custom glyphs, in blocks
     * per unit of Java display scale. Applied on top of the text anchor
     * correction, independently of the Bedrock glyph-size adjustment.
     */
    private static final float BEDROCK_SINGLE_GLYPH_VERTICAL_OFFSET = -0.25f;

    /**
     * On Java Edition, armor stands can have a custom name shown additionally to
     * the text in the display. They are rendered separately, and can cross each other...
     */
    private @Nullable ArmorStandEntity secondEntity = null;
    private boolean isInvisible = false;
    private boolean sendUpdates = false;

    @Getter
    private @NonNull Component text = Component.empty();

    @Getter
    private @NonNull Color backgroundColor = new Color(0, 0, 0, 40);

    @Getter
    private byte textOpacity = -1;

    @Getter
    private boolean defaultBackground = false;

    @Getter
    private boolean shadow = false;

    @Getter
    private boolean seeThrough = false;

    public TextDisplayEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        // Remove armor stand body / hitbox
        this.metadata.put(EntityDataTypes.HITBOX, NbtMap.EMPTY);
        this.metadata.put(EntityDataTypes.SCALE, 0f);
    }

    @Override
    protected void setInvisible(boolean value) {
        // we'll keep the text display armor stand always invisible; would reveal the armor stand otherwise
        // but we would need to adjust the nametag
        isInvisible = value;
        this.updateNameTag();
    }

    @Override
    public void setCustomNameVisible(BooleanEntityMetadata entityMetadata) {
        super.setCustomNameVisible(entityMetadata);
        this.updateNameTag();
    }

    @Override
    public void setCustomName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        super.setCustomName(entityMetadata);
        this.updateNameTag();
    }

    @Override
    public void setNametagAlwaysShow(boolean value) {
        // no-op
    }

    @Override
    protected void setNameEntityData(String nametag) {
        // no-op
    }

    @Override
    public void setScale(EntityMetadata<Vector3f, ?> translationMeta) {
        super.setScale(translationMeta);

        updatePrimitiveText();
    }

    @Override
    public void setLeftRotation(EntityMetadata<Quaternionf, ?> metadata) {
        super.setLeftRotation(metadata);

        updatePrimitiveText();
    }

    @Override
    public void setRightRotation(EntityMetadata<Quaternionf, ?> metadata) {
        super.setRightRotation(metadata);

        updatePrimitiveText();
    }

    public void setTextMetadata(EntityMetadata<Component, ?> metadata) {
        this.text = metadata.getValue();

        updatePrimitiveText();
    }

    public void setBackgroundColor(IntEntityMetadata metadata) {
        int value = metadata.getPrimitiveValue();

        int a = (value >> 24) & 0xFF;
        int r = (value >> 16) & 0xFF;
        int g = (value >> 8) & 0xFF;
        int b = value & 0xFF;

        this.backgroundColor = new Color(r, g, b, a);

        updatePrimitiveText();
    }

    public void setTextOpacity(ByteEntityMetadata metadata) {
        this.textOpacity = metadata.getPrimitiveValue();

        updatePrimitiveText();
    }

    // Java TextDisplay metadata flags (Minecraft 26.3):
    // 0x01 = shadow, 0x02 = see through blocks, 0x04 = default background.
    // Do not mistake default-background (0x04) for see-through; that disables
    // Bedrock DebugText depth testing and lets holograms show through walls.
    static final int STYLE_FLAG_SHADOW = 0x01;
    static final int STYLE_FLAG_SEE_THROUGH = 0x02;
    static final int STYLE_FLAG_DEFAULT_BACKGROUND = 0x04;

    static boolean hasStyleFlag(byte flags, int mask) {
        return (flags & mask) != 0;
    }

    /**
     * Java TextDisplay's see_through flag and Bedrock DebugText's depthTest
     * property have opposite meanings. Keep this mapping in one place so
     * default, shadowed, and background-only text remains block-occluded.
     */
    static void applySeeThroughToBedrock(DebugText bedrockText, boolean javaSeeThrough) {
        bedrockText.setDepthTest(!javaSeeThrough);
    }

    public void setStyleFlags(ByteEntityMetadata metadata) {
        byte flags = metadata.getPrimitiveValue();

        this.shadow = hasStyleFlag(flags, STYLE_FLAG_SHADOW);
        this.seeThrough = hasStyleFlag(flags, STYLE_FLAG_SEE_THROUGH);
        this.defaultBackground = hasStyleFlag(flags, STYLE_FLAG_DEFAULT_BACKGROUND);

        updatePrimitiveText();
    }

    @Override
    public void spawnEntity() {
        super.spawnEntity();

        sendUpdates = true;
        updatePrimitiveText();
    }

    @Override
    public void sendSpectateInvisible(boolean hidden) {
        super.sendSpectateInvisible(hidden);
        // TODO handle this, we probably just wanna hide the primitive text?
    }

    @Override
    public void despawnEntity() {
        sendUpdates = false;
        if (secondEntity != null) {
            secondEntity.despawnEntity();
        }
        super.despawnEntity();

        DebugDrawerPacket packet = new DebugDrawerPacket();
        packet.getShapes().add(new DebugShape(geyserId));
        session.sendUpstreamPacket(packet);
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        super.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);

        if (secondEntity != null) {
            secondEntity.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        }
        updatePrimitiveText();
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        super.moveAbsoluteRaw(position, yaw, pitch, headYaw, isOnGround, teleported);

        if (secondEntity != null) {
            secondEntity.moveAbsoluteRaw(position.down(LINE_HEIGHT_OFFSET), yaw, pitch, headYaw, isOnGround, teleported);
        }
        updatePrimitiveText();
    }

    @Override
    public void updateBedrockMetadata() {
        super.updateBedrockMetadata();

        // Bundle metadata updates to ensure they aren't ignored
        if (secondEntity != null) {
            if (!secondEntity.valid) { // Spawn the entity once
                secondEntity.spawnEntity();
            } else {
                secondEntity.updateBedrockMetadata();
            }
        }
    }

    public void updatePrimitiveText() {
        if (!sendUpdates) return;
        // Find the message and color
        String message;
        Color color = null;

        Set<Color> colors = ColorUtils.extractAllColors(text);

        if (colors.size() < 2) {
            message = MessageTranslator.convertNoColorMessage(text, session.locale());
            color = colors.isEmpty() ? null : colors.iterator().next();
        } else {
            message = MessageTranslator.convertMessage(text, session.locale());
        }

        // Retain empty lines which Bedrock's debug-text layout may collapse.
        message = preserveBlankLines(message);

        // Calculate a matrix then extract components

        // The attached Bedrock entity is already positioned at baseTranslation by
        // DisplayBaseEntity.bedrockPosition(). Adding it again here doubles the offset.
        Matrix4f matrix = Matrix4f.IDENTITY
            .rotate(getLeftRotation())
            .scale(Vector4f.from(getBaseScale(), 1))
            .rotate(getRightRotation());

        float m00, m01, m02, m10, m11, m12, m20, m21, m22;
        m00 = matrix.get(0, 0);
        m01 = matrix.get(0, 1);
        m02 = matrix.get(0, 2);
        m10 = matrix.get(1, 0);
        m11 = matrix.get(1, 1);
        m12 = matrix.get(1, 2);
        m20 = matrix.get(2, 0);
        m21 = matrix.get(2, 1);
        m22 = matrix.get(2, 2);

        float sx = (float) Math.sqrt(m00*m00 + m10*m10 + m20*m20);
        float sy = (float) Math.sqrt(m01*m01 + m11*m11 + m21*m21);
        float sz = (float) Math.sqrt(m02*m02 + m12*m12 + m22*m22);

        float r00 = m00 / sx, r10 = m10 / sx, r20 = m20 / sx;
        float r01 = m01 / sy, r11 = m11 / sy, r21 = m21 / sy;
        float r02 = m02 / sz, r12 = m12 / sz, r22 = m22 / sz;

        float x = (float) Math.atan2(r21, r22);
        float y = (float) Math.atan2(-r20, Math.hypot(r00, r10));
        float z = (float) Math.atan2(r10, r00);

        float scale = MathUtils.average(new float[]{ sx, sy, sz });

        // I'm not even gonna pretend I know why x needs to be negative, but it does, and it works
        Vector3f rotation = Vector3f.from(-Math.toDegrees(x), Math.toDegrees(y), Math.toDegrees(z));

        DebugText text = new DebugText();
        text.setId(geyserId);
        text.setText(message);
        text.setColor(
            color == null ? null :
                new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    textOpacity < 0 ? (textOpacity + 256) : textOpacity
                )
        );

        // Java renders upward from its bottom-center pivot, while Bedrock DebugText
        // is center-anchored. Java uses a 9-pixel glyph height and 1-pixel line gap.
        // Count the translated message that we actually submit to Bedrock, not
        // the raw component; translation and trailing empty lines can alter it.
        int lineCount = message.split("\\n", -1).length;
        String rawText = PlainTextComponentSerializer.plainText().serialize(this.text);
        float baseVerticalOffset = calculateBedrockVerticalOffset(lineCount, scale);
        float customGlyphOffset = singleGlyphVerticalOffset(rawText, scale, BEDROCK_SINGLE_GLYPH_VERTICAL_OFFSET);
        float verticalOffset = baseVerticalOffset + customGlyphOffset;
        text.setPosition(Vector3f.from(0, verticalOffset, 0));
        text.setAttachedToEntityId(geyserId);

        Vector3f outputRotation = Vector3f.from(
            0, (-getYaw()) + 180, 0
        );

        // Combine the entity's world yaw with its display transform rotation.
        outputRotation = outputRotation.add(rotation);

        text.setRotation(outputRotation);
        text.setUseRotation(!getBillboard().equals(Billboard.CENTER));
        text.setMaximumRenderDistance(getViewDistance());
        text.setBackgroundColor(backgroundColor);
        text.setDepthTest(!seeThrough);
        //text.setLineGapHeight();
        //text.setShowBackface();
        //text.setShowTextBackface();
        text.setDimension(session.getBedrockDimension().bedrockId());

        // Apply a separate correction only to a text display containing one
        // private-use font icon. Keep the Java-scale pivot correction above:
        // changing the glyph's pixel size must not move its attachment anchor.
        float bedrockScale = adjustedGlyphScale(rawText, scale, BEDROCK_SINGLE_GLYPH_SCALE);
        text.setScale(bedrockScale);

        DebugDrawerPacket packet = new DebugDrawerPacket();
        packet.getShapes().add(text);
        session.sendUpstreamPacket(packet);
    }

    /**
     * Align the vertical centre of Bedrock's DebugText with Java's bottom
     * text-display pivot. Java uses 9-pixel glyphs plus 1-pixel line spacing
     * and 40 pixels per world block. Only Java's text height scales with
     * the display; Bedrock's attached-entity baseline correction is in
     * world units and must remain constant across font scales.
     */
    static float calculateBedrockVerticalOffset(int lineCount, float scale) {
        if (lineCount <= 0) {
            return 0.0f;
        }
        float javaTextHeight = (lineCount * 10.0f - 1.0f) * JAVA_TEXT_PIXEL_SCALE;
        return javaTextHeight * scale / 2.0f + BEDROCK_TEXT_ANCHOR_CORRECTION;
    }

    /**
     * A single private-use Unicode code point represents a custom-font icon.
     * For these displays the Bedrock bitmap-font page has a different native
     * glyph scale than the Java font provider. Correct the visual glyph size
     * without modifying the source entity's transform or other text displays.
     */
    static float adjustedGlyphScale(String plainText, float javaScale, float correction) {
        return isSinglePrivateUseGlyph(plainText) ? javaScale * correction : javaScale;
    }

    /**
     * Apply a custom-font-specific baseline offset on top of the standard Java
     * line-height anchor. The delta follows the original Java transform scale
     * (not the additionally corrected Bedrock font scale) so a glyph at 1.5x
     * moves 1.5 times as far as the same font at 1x.
     */
    static float singleGlyphVerticalOffset(String plainText, float javaScale, float offsetPerScale) {
        return isSinglePrivateUseGlyph(plainText) ? javaScale * offsetPerScale : 0.0f;
    }

    private static boolean isSinglePrivateUseGlyph(String plainText) {
        if (plainText == null || plainText.isEmpty() || plainText.codePointCount(0, plainText.length()) != 1) {
            return false;
        }
        return Character.getType(plainText.codePointAt(0)) == Character.PRIVATE_USE;
    }

    /**
     * Keep explicit empty lines visible in the Bedrock DebugText layout.
     * A non-breaking space occupies the line without adding visible lettering.
     */
    static String preserveBlankLines(String message) {
        String[] lines = message.replace("\r\n", "\n").split("\n", -1);
        StringBuilder result = new StringBuilder(message.length());
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(lines[i].isEmpty() ? "\u00a0" : lines[i]);
        }
        return result.toString();
    }

    public void updateNameTag() {
        // Text displays are special: customNameVisible must be set for the custom name to ever show
        if (this.nametag.isBlank() || isInvisible || !customNameVisible) {
            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
            return;
        }

        if (this.secondEntity == null) {
            secondEntity = new ArmorStandEntity(EntitySpawnContext.inherited(session, VanillaEntities.ARMOR_STAND, this, position.down(LINE_HEIGHT_OFFSET)));
        }
        secondEntity.getMetadata().put(EntityDataTypes.NAME, this.nametag);
        secondEntity.getMetadata().put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) 1);
        // Scale to 0 to show nametag
        secondEntity.setScale(0f);
        // No bounding box as we don't want to interact with this entity
        secondEntity.getMetadata().put(EntityDataTypes.WIDTH, 0.0f);
        secondEntity.getMetadata().put(EntityDataTypes.HEIGHT, 0.0f);
        secondEntity.getMetadata().put(EntityDataTypes.HITBOX, NbtMap.EMPTY);
    }
}
