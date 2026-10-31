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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.entity.VanillaEntities;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.ColorUtils;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.geyser.util.SignUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
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

    // Both editions use 10 font pixels per line including background padding, but Bedrock's font pixels are larger
    private static final float JAVA_LINE_HEIGHT = 0.25f; // 10px at 0.025 blocks per pixel
    private static final float BEDROCK_LINE_HEIGHT = 0.266071429f; // measured at scale 1
    private static final float BEDROCK_SCALE_FACTOR = JAVA_LINE_HEIGHT / BEDROCK_LINE_HEIGHT;

    /**
     * Java anchors the text display at the bottom of its background, whereas Bedrock anchors the first line of primitive text
     * 10% of its height below the top and scales from there.
     */
    private static final float BEDROCK_ANCHOR_OFFSET = JAVA_LINE_HEIGHT * 0.2f;

    private static final boolean DEBUG = Boolean.getBoolean("Geyser.Debug.TextDisplay");

    /**
     * On Java Edition, armor stands can have a custom name shown additionally to
     * the text in the display. They are rendered separately, and can cross each other...
     */
    private @Nullable ArmorStandEntity secondEntity = null;
    private boolean isInvisible = false;
    private boolean sendUpdates = false;

    @Getter
    private @NonNull String unprocessedText = "";

    @Getter
    private @NonNull String text = "";

    @Getter
    private Color color = null;

    @Getter
    private int lineWidth = 200;

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
        Component text = metadata.getValue();

        String message;
        Color color = null;

        Set<Color> colors = ColorUtils.extractAllColors(text);

        if (colors.size() < 2) {
            message = MessageTranslator.convertNoColorMessage(text, session.locale());
            color = colors.isEmpty() ? null : colors.iterator().next();
        } else {
            message = MessageTranslator.convertMessage(text, session.locale());
        }

        this.unprocessedText = message;
        this.color = color;

        processText();
        updatePrimitiveText();
    }

    public void setLineWidth(IntEntityMetadata metadata) {
        this.lineWidth = metadata.getPrimitiveValue();
        processText();
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

    public void setStyleFlags(ByteEntityMetadata metadata) {
        byte flags = metadata.getPrimitiveValue();

        this.defaultBackground = (flags & 0x01) == 1;
        this.shadow = ((flags >> 1) & 0x01) == 1;
        this.seeThrough = ((flags >> 2) & 0x01) == 1;

        updatePrimitiveText();
    }

    private void processText() {
        this.text = postProcessText(this.unprocessedText);
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

        // Calculate a matrix then extract components
        Matrix4f matrix = Matrix4f.IDENTITY.translate(getBaseTranslation())
            .rotate(getLeftRotation())
            .scale(Vector4f.from(getBaseScale(), 1))
            .rotate(getRightRotation());

        Vector3f translation = Vector3f.from(matrix.get(3, 0), matrix.get(3, 1), matrix.get(3, 2));

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

        float scale = MathUtils.average(new float[]{ sx, sy }) * BEDROCK_SCALE_FACTOR; // sz doesn't affect the java scale, so don't use it

        // Move the bedrock anchor to where the top line sits within the java text, in the java text's local space
        int lines = text.split("\n", -1).length;
        Matrix4f transform = Matrix4f.createRotation(getLeftRotation())
            .mul(Matrix4f.createScaling(Vector4f.from(getBaseScale(), 1)))
            .mul(Matrix4f.createRotation(getRightRotation()));
        Vector4f anchor = transform.transform(0, JAVA_LINE_HEIGHT * lines - BEDROCK_ANCHOR_OFFSET, 0, 1);
        translation = translation.add(anchor.getX(), anchor.getY(), anchor.getZ());

        // Bedrock moves the text up by half of an unscaled line for every extra line, so undo that along the text's up axis
        if (lines > 1) {
            Vector4f up = transform.transform(0, 1, 0, 0).normalize().mul(BEDROCK_LINE_HEIGHT * (lines - 1) / 2);
            translation = translation.sub(up.getX(), up.getY(), up.getZ());
        }

        // I'm not even gonna pretend I know why x needs to be negative, but it does, and it works
        Vector3f rotation = Vector3f.from(-Math.toDegrees(x), Math.toDegrees(y), Math.toDegrees(z));

        DebugText text = new DebugText();
        text.setId(geyserId);
        text.setText(DEBUG ? String.valueOf(geyserId) : this.text);
        text.setColor(
            color == null ? null :
                new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    textOpacity < 0 ? (textOpacity + 256) : textOpacity
                )
        );
        text.setPosition(translation);
        text.setAttachedToEntityId(geyserId);

        GeyserLogger log = GeyserImpl.getInstance().getLogger();

        Vector3f outputRotation = Vector3f.from(
            0, (-getYaw()) + 180, 0
        );

        if (DEBUG) {
            log.info("Text Display Debug Update (" + geyserId + "):");
            log.info("- Rotation:");
            log.info("  - Yaw: " + outputRotation);
        }

//        Vector3f leftRotation = toVector(this.getLeftRotation());
//
//        if (DEBUG) {
//            log.info("  - Left Rotation: " + leftRotation);
//        }
//
//        // Add left rotation
//        outputRotation = outputRotation.add(leftRotation);
//
//        if (DEBUG) {
//            log.info("  - Yaw + Left Rotation: " + outputRotation);
//        }

        if (DEBUG) {
            log.info("  - Rotation: " + rotation);
        }

        // Add rotation
        outputRotation = outputRotation.add(rotation);

        if (DEBUG) {
            log.info("  - Yaw + Rotation: " + outputRotation);
        }

        text.setRotation(outputRotation);
        text.setUseRotation(!getBillboard().equals(Billboard.CENTER));
        text.setMaximumRenderDistance(getViewDistance());
        text.setBackgroundColor(backgroundColor);
        text.setDepthTest(!seeThrough);
        //text.setLineGapHeight();
        //text.setShowBackface();
        //text.setShowTextBackface();
        text.setDimension(session.getBedrockDimension().bedrockId());
        text.setScale(scale);
        DebugDrawerPacket packet = new DebugDrawerPacket();
        packet.getShapes().add(text);
        session.sendUpstreamPacket(packet);
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

    private String postProcessText(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.length() == 2 && line.startsWith(String.valueOf(ChatColor.ESCAPE))) {
                lines.add("\u00a0");
                continue;
            }
            wrapLine(line, lines);
        }

        return String.join("\n", lines);
    }

    private void wrapLine(String line, List<String> lines) {
        String prefix = "";
        String formatting = "";
        boolean bold = false;
        int start = 0;
        int width = 0;

        int lastSpace = -1;
        String formattingAtSpace = "";
        boolean boldAtSpace = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ChatColor.ESCAPE && i + 1 < line.length()) {
                char code = Character.toLowerCase(line.charAt(++i));
                if (code == 'r') {
                    formatting = "";
                    bold = false;
                } else if (Character.digit(code, 16) != -1) {
                    // Colors reset any other formatting
                    formatting = ChatColor.ESCAPE + String.valueOf(code);
                    bold = false;
                } else {
                    formatting += ChatColor.ESCAPE + String.valueOf(code);
                    bold |= code == 'l';
                }
                continue;
            }

            if (c == ' ') {
                lastSpace = i;
                formattingAtSpace = formatting;
                boldAtSpace = bold;
            }

            int charWidth = SignUtils.getCharacterWidth(c) + (bold ? 1 : 0);
            if (width > 0 && width + charWidth > lineWidth) {
                if (lastSpace > start) {
                    // Break at the last space, which Java drops
                    lines.add(prefix + line.substring(start, lastSpace));
                    start = lastSpace + 1;
                    formatting = formattingAtSpace;
                    bold = boldAtSpace;
                } else {
                    lines.add(prefix + line.substring(start, i));
                    start = i;
                }
                prefix = formatting;
                width = 0;
                lastSpace = -1;
                // Rescan from the start of the new line
                i = start - 1;
                continue;
            }
            width += charWidth;
        }
        lines.add(prefix + line.substring(start));
    }
}
