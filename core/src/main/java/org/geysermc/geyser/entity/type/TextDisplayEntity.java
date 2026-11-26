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
import net.kyori.adventure.text.TextComponent;
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
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Optional;
import java.util.Set;

// Note: 1.19.4 requires that the billboard is set to something in order to show, on Java Edition
@Getter
public class TextDisplayEntity extends DisplayBaseEntity {

    private static final boolean DEBUG = Boolean.getBoolean("Geyser.DebugTextDisplays");

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
    private static final float BEDROCK_ANCHOR_OFFSET = JAVA_LINE_HEIGHT * 0.1f;

    private static final Color DEFAULT_BACKGROUND = new Color(0, 0, 0, 64);

    /**
     * On Java Edition, armor stands can have a custom name shown additionally to
     * the text in the display. They are rendered separately, and can cross each other...
     */
    private @Nullable ArmorStandEntity secondEntity = null;
    private boolean isInvisible = false;
    private boolean sendUpdates = false;

    private @NonNull String unprocessedText = "";

    private @NonNull String text = "";

    private Color color = null;

    private int lineWidth = 200;

    private @NonNull Color backgroundColor = DEFAULT_BACKGROUND;

    private byte textOpacity = -1;

    private boolean defaultBackground = false;

    private boolean shadow = false;

    private boolean seeThrough = false;

    private boolean hidden = false;

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

    public void setTextMetadata(EntityMetadata<Component, ?> metadata) {
        Component text = metadata.getValue();

        String message;
        Color color = null;

        Set<Color> colors = ColorUtils.extractAllColors(text);

        // Only tint the whole text when every part has the color, otherwise uncolored parts would get it too
        if (colors.isEmpty() || (colors.size() == 1 && isFullyColored(text, false))) {
            message = MessageTranslator.convertNoColorMessage(text, session.locale());
            color = colors.isEmpty() ? null : colors.iterator().next();
        } else {
            message = MessageTranslator.convertMessage(text, session.locale());
        }

        this.unprocessedText = message;
        this.color = color;

        processText();
    }

    private static boolean isFullyColored(Component component, boolean inheritedColor) {
        boolean colored = inheritedColor || component.color() != null;
        if (!colored) {
            if (!(component instanceof TextComponent textComponent) || !textComponent.content().isEmpty()) {
                return false;
            }
        }
        for (Component child : component.children()) {
            if (!isFullyColored(child, colored)) {
                return false;
            }
        }
        return true;
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
        setDisplayDataDirty();
    }

    public void setTextOpacity(ByteEntityMetadata metadata) {
        this.textOpacity = metadata.getPrimitiveValue();
        setDisplayDataDirty();
    }

    public void setStyleFlags(ByteEntityMetadata metadata) {
        byte flags = metadata.getPrimitiveValue();

        this.shadow = (flags & 0x01) == 1;
        this.seeThrough = ((flags >> 1) & 0x01) == 1;
        this.defaultBackground = ((flags >> 2) & 0x01) == 1;
        setDisplayDataDirty();
    }

    private void processText() {
        this.text = MessageTranslator.wrapLines(this.unprocessedText, this.lineWidth);
        setDisplayDataDirty();
    }

    @Override
    public void spawnEntity() {
        super.spawnEntity();

        sendUpdates = true;

        if (!hidden) { // Only update if the display is shown
            updatePrimitiveText();
        }
    }

    @Override
    public void sendSpectateInvisible(boolean hidden) {
        super.sendSpectateInvisible(hidden);

        boolean previousHidden = this.hidden;

        this.hidden = hidden;

        if (this.hidden != previousHidden && sendUpdates) {
            if (hidden) { // Remove the display
                DebugDrawerPacket packet = new DebugDrawerPacket();
                packet.getShapes().add(new DebugShape(geyserId));
                session.sendUpstreamPacket(packet);
            } else { // Update the text for any changes and to show
                updatePrimitiveText();
            }
        }
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
        float oldYaw = this.yaw;
        float oldPitch = this.pitch;
        super.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);

        if (secondEntity != null) {
            secondEntity.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        }

        if (getBillboard() != Billboard.CENTER && (yaw != oldYaw || pitch != oldPitch)) updatePrimitiveText();
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        float oldYaw = this.yaw;
        float oldPitch = this.pitch;
        super.moveAbsoluteRaw(position, yaw, pitch, headYaw, isOnGround, teleported);

        if (secondEntity != null) {
            secondEntity.moveAbsoluteRaw(position.down(LINE_HEIGHT_OFFSET), yaw, pitch, headYaw, isOnGround, teleported);
        }

        if (getBillboard() != Billboard.CENTER && (yaw != oldYaw || pitch != oldPitch)) updatePrimitiveText();
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

        if (isDisplayDataDirty()) {
            // Metadata setters only store their values, so a whole metadata packet results in a single shape update
            updatePrimitiveText();
        }
    }

    @Override
    public Vector3f bedrockPosition() {
        // This *needs* to be handled differently for text displays, otherwise riding is messed up
        return super.bedrockPosition().sub(getBaseTranslation());
    }

    @Override
    public void setVehicle(Entity vehicle) {
        super.setVehicle(vehicle);

        // This is important, there are some cases where this method is called and
        // EntityUtils#updateMountOffset isn't, so we'll mark here regardless in order
        // to ensure an update is pushed
        setDisplayDataDirty();
    }

    public void updatePrimitiveText() {
        if (!sendUpdates || hidden) return;

        // Called here as this method can be called without checking the dirty flag (#setVehicle(Entity)),
        // *but* this method will **always** clear the dirty flag, so it should do so
        clearDisplayDataDirty();

        DebugText text = new DebugText();
        applyTransformation(text);
        text.setId(geyserId);
        // Text opacity still has to apply when the colors come from legacy codes, -1 is the fully opaque default
        Color baseColor = color == null && textOpacity != -1 ? Color.WHITE : color;
        text.setColor(
            baseColor == null ? null :
                new Color(
                    baseColor.getRed(),
                    baseColor.getGreen(),
                    baseColor.getBlue(),
                    textOpacity < 0 ? (textOpacity + 256) : textOpacity
                )
        );
        if (getVehicle() != null) {
            text.setAttachedToEntityId(getVehicle().geyserId);
            text.setPosition(text.getPosition().add(0, getVehicle().getBoundingBoxHeight() - 0.2f, 0));
        } else {
            text.setAttachedToEntityId(geyserId);
        }
        text.setUseRotation(!getBillboard().equals(Billboard.CENTER));
        // Java's view range is a multiplier of 64 blocks, Bedrock wants the distance itself
        text.setMaximumRenderDistance(getViewDistance() < 0 ? 0 : getViewDistance() * 64);
        text.setBackgroundColor(defaultBackground ? DEFAULT_BACKGROUND : backgroundColor);
        text.setDepthTest(!seeThrough);
        text.setDimension(session.getBedrockDimension().bedrockId());

        if (DEBUG) {
            Vector3f position = bedrockPosition();
            Vector3f offset = text.getPosition();
            text.setText("ID: " + geyserId + ", Pos: " + position.getX() + ", " + position.getY() + ", " + position.getZ() + ", Off: " + offset.getX() + ", " + offset.getY() + ", " + offset.getZ());
        } else {
            text.setText(this.text);
        }

        DebugDrawerPacket packet = new DebugDrawerPacket();
        packet.getShapes().add(text);
        session.sendUpstreamPacket(packet);
    }

    private void applyTransformation(DebugText text) {
        // Create the matrix
        // Java's transformation is T * L * S * R. Cloudburst's translate/rotate/scale pre-multiply, so build it with mul()
        // instead. The translation is left out here and added at the end.
        Matrix4f LSR = Matrix4f.createRotation(getLeftRotation())
            .mul(Matrix4f.createScaling(Vector4f.from(getBaseScale(), 1)))
            .mul(Matrix4f.createRotation(getRightRotation()));

        // Apply yaw, pitch, the LSR matrix then finally 180 on the Y axis, this is how java handles this
        Matrix4f matrix = Matrix4f.createRotation(Quaternionf.fromAngleDegAxis(-getYaw(), Vector3f.UNIT_Y))
            .mul(Matrix4f.createRotation(Quaternionf.fromAngleDegAxis(getPitch(), Vector3f.UNIT_X)))
            .mul(LSR)
            .mul(Matrix4f.createRotation(Quaternionf.fromAngleDegAxis(180, Vector3f.UNIT_Y)));

        // Extract the matrix elements
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

        // Apply scale
        double sx = Math.sqrt(m00*m00 + m10*m10 + m20*m20);
        double sy = Math.sqrt(m01*m01 + m11*m11 + m21*m21);
        double sz = Math.sqrt(m02*m02 + m12*m12 + m22*m22);

        // sz doesn't affect the java scale with text displays, so don't use it
        if (sx == 0 || sy == 0) {
            // One of these 2 being 0 will hide the display, so let's skip the maths which could
            // make the display visible when it shouldn't be!
            text.setScale(0f);
        } else {
            float scale = (float) (((sx + sy) / 2) * BEDROCK_SCALE_FACTOR);

            text.setScale(scale);
        }

        // Apply rotation
        // A zero scale (often used to hide a display) would divide by zero and send NaN rotations to the client
        // although this wouldn't be an issue (since a 0 scale hides it, so who cares), we'll account anyway
        double ix = sx == 0 ? 0 : 1 / sx, iy = sy == 0 ? 0 : 1 / sy, iz = sz == 0 ? 0 : 1 / sz;
        double r00 = m00 * ix, r10 = m10 * ix, r20 = m20 * ix;
        double r01 = m01 * iy, r11 = m11 * iy, r21 = m21 * iy;
        double r02 = m02 * iz, r12 = m12 * iz, r22 = m22 * iz;

        double x = Math.atan2(r21, r22);
        double y = Math.atan2(-r20, Math.hypot(r00, r10));
        double z = Math.atan2(r10, r00);

        text.setRotation(Vector3f.from(Math.toDegrees(x), Math.toDegrees(y), Math.toDegrees(z)));

        // Apply translation
        // Move the bedrock anchor to where the top line sits within the java text, in the java text's local space
        int lines = this.text.split("\n", -1).length;
        Vector4f anchor = matrix.transform(0, JAVA_LINE_HEIGHT * lines - BEDROCK_ANCHOR_OFFSET, 0, 1);
        Vector3f translation = Vector3f.from(anchor.getX(), anchor.getY(), anchor.getZ());

        // Bedrock moves the text up by half of an unscaled line for every extra line, so undo that along the text's up axis
        if (lines > 1 && sy != 0) {
            Vector4f up = matrix.transform(0, 1, 0, 0).normalize().mul(BEDROCK_LINE_HEIGHT * (lines - 1) / 2);
            translation = translation.sub(up.getX(), up.getY(), up.getZ());
        }

        text.setPosition(translation.add(getBaseTranslation()));
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
