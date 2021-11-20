/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living.animal.tameable;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import com.google.common.collect.ImmutableSet;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.Set;
import java.util.UUID;

public class WolfEntity extends TameableEntity {
    /**
     * A list of all foods a wolf can eat on Java Edition.
     * Used to display interactive tag or particles if needed.
     */
    private static final Set<String> WOLF_FOODS = ImmutableSet.of("pufferfish", "tropical_fish", "chicken", "cooked_chicken",
            "porkchop", "beef", "rabbit", "cooked_porkchop", "cooked_beef", "rotten_flesh", "mutton", "cooked_mutton",
            "cooked_rabbit");

    private byte collarColor;

    public WolfEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    public void setTameableFlags(ByteEntityMetadata entityMetadata) {
        super.setTameableFlags(entityMetadata);
        // Reset wolf color
        byte xd = entityMetadata.getPrimitiveValue();
        boolean angry = (xd & 0x02) == 0x02;
        if (angry) {
            dirtyMetadata.put(EntityData.COLOR, (byte) 0);
        }
    }

    public void setCollarColor(IntEntityMetadata entityMetadata) {
        collarColor = (byte) entityMetadata.getPrimitiveValue();
        if (getFlag(EntityFlag.ANGRY)) {
            return;
        }

        dirtyMetadata.put(EntityData.COLOR, collarColor);
        if (ownerBedrockId == 0) {
            // If a color is set and there is no owner entity ID, set one.
            // Otherwise, the entire wolf is set to that color: https://user-images.githubusercontent.com/9083212/99209989-92691200-2792-11eb-911d-9a315c955be9.png
            dirtyMetadata.put(EntityData.OWNER_EID, session.getPlayerEntity().getGeyserId());
        }
    }

    // 1.16+
    public void setWolfAngerTime(IntEntityMetadata entityMetadata) {
        int time = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.ANGRY, time != 0);
        dirtyMetadata.put(EntityData.COLOR, time != 0 ? (byte) 0 : collarColor);
    }

    @Override
    public boolean canEat(String javaIdentifierStripped, ItemMapping mapping) {
        // Cannot be a baby to eat these foods
        return WOLF_FOODS.contains(javaIdentifierStripped) && !isBaby();
    }
}
