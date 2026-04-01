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
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


@Getter
public class TextDisplayEntity extends DisplayBaseEntity {

    
    private static final float LINE_HEIGHT_OFFSET = 0.1414f;

    
    private @Nullable ArmorStandEntity secondEntity = null;
    private boolean isInvisible = false;
    private int lineCount;

    public TextDisplayEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        
        this.dirtyMetadata.put(EntityDataTypes.HITBOX, NbtMap.EMPTY);
        this.dirtyMetadata.put(EntityDataTypes.SCALE, 0f);
        this.dirtyMetadata.put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) 1);
    }

    @Override
    protected void setInvisible(boolean value) {
        
        
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
        
    }

    @Override
    protected void setNameEntityData(String nametag) {
        
    }

    @Override
    public void despawnEntity() {
        if (secondEntity != null) {
            secondEntity.despawnEntity();
        }
        super.despawnEntity();
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        if (secondEntity != null) {
            secondEntity.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        }
        super.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        if (secondEntity != null) {
            secondEntity.moveAbsoluteRaw(position.down(LINE_HEIGHT_OFFSET), yaw, pitch, headYaw, isOnGround, teleported);
        }
        super.moveAbsoluteRaw(position, yaw, pitch, headYaw, isOnGround, teleported);
    }

    public void setText(EntityMetadata<Component, ?> entityMetadata) {
        this.dirtyMetadata.put(EntityDataTypes.NAME, MessageTranslator.convertMessage(entityMetadata.getValue(), session.locale()));
        int oldLineCount = this.lineCount;
        this.lineCount = calculateLineCount(entityMetadata.getValue());

        
        if (this.lineCount != oldLineCount) {
            setOffset(calculateLineOffset());
            moveAbsoluteRaw(position, yaw, pitch, headYaw, onGround, false);
        }
    }

    private int calculateLineCount(@Nullable Component text) {
        if (text == null) {
            return 0;
        }
        return PlainTextComponentSerializer.plainText().serialize(text).split("\n").length;
    }

    @Override
    public void updateBedrockMetadata() {
        
        if (secondEntity != null) {
            if (!secondEntity.valid) { 
                secondEntity.spawnEntity();
            } else {
                secondEntity.updateBedrockMetadata();
            }
        }
        super.updateBedrockMetadata();
    }

    public void updateNameTag() {
        
        if (this.nametag.isBlank() || isInvisible || !customNameVisible) {
            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
            return;
        }

        if (this.secondEntity == null) {
            secondEntity = new ArmorStandEntity(EntitySpawnContext.inherited(session, EntityDefinitions.ARMOR_STAND, this, position.down(LINE_HEIGHT_OFFSET)));
        }
        secondEntity.getDirtyMetadata().put(EntityDataTypes.NAME, this.nametag);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) 1);
        
        secondEntity.setScale(0f);
        
        secondEntity.getDirtyMetadata().put(EntityDataTypes.WIDTH, 0.0f);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.HEIGHT, 0.0f);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.HITBOX, NbtMap.EMPTY);
    }
    
    public float calculateLineOffset() {
        if (lineCount == 0) {
            return 0;
        }
        return -0.6f + LINE_HEIGHT_OFFSET * lineCount;
    }
}
