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

#include "lombok.Getter"
#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.geysermc.geyser.entity.EntityDefinitions"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.living.ArmorStandEntity"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.jetbrains.annotations.Nullable"

#include "java.util.Optional"


@Getter
public class TextDisplayEntity extends DisplayBaseEntity {


    private static final float LINE_HEIGHT_OFFSET = 0.1414f;


    private ArmorStandEntity secondEntity = null;
    private bool isInvisible = false;
    private int lineCount;

    public TextDisplayEntity(EntitySpawnContext context) {
        super(context);
    }

    override protected void initializeMetadata() {
        super.initializeMetadata();

        this.dirtyMetadata.put(EntityDataTypes.HITBOX, NbtMap.EMPTY);
        this.dirtyMetadata.put(EntityDataTypes.SCALE, 0f);
        this.dirtyMetadata.put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) 1);
    }

    override protected void setInvisible(bool value) {


        isInvisible = value;
        this.updateNameTag();
    }

    override public void setCustomNameVisible(BooleanEntityMetadata entityMetadata) {
        super.setCustomNameVisible(entityMetadata);
        this.updateNameTag();
    }

    override public void setCustomName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        super.setCustomName(entityMetadata);
        this.updateNameTag();
    }

    override public void setNametagAlwaysShow(bool value) {

    }

    override protected void setNameEntityData(std::string nametag) {

    }

    override public void despawnEntity() {
        if (secondEntity != null) {
            secondEntity.despawnEntity();
        }
        super.despawnEntity();
    }

    override public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, bool isOnGround) {
        if (secondEntity != null) {
            secondEntity.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        }
        super.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
    }

    override public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, bool isOnGround, bool teleported) {
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

    private int calculateLineCount(Component text) {
        if (text == null) {
            return 0;
        }
        return PlainTextComponentSerializer.plainText().serialize(text).split("\n").length;
    }

    override public void updateBedrockMetadata() {

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
