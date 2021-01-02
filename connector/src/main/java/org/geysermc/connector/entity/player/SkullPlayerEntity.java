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

package org.geysermc.connector.entity.player;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.network.session.GeyserSession;

/**
 * A wrapper to handle skulls more effectively - skulls have to be treated as entities since there are no
 * custom player skulls in Bedrock.
 */
public class SkullPlayerEntity extends PlayerEntity {

    /**
     * Stores the block state that the skull is associated with. Used to determine if the block in the skull's position
     * has changed
     */
    @Getter
    @Setter
    private int blockState;

    public SkullPlayerEntity(GameProfile gameProfile, long geyserId, Vector3f position, Vector3f rotation) {
        super(gameProfile, 0, geyserId, position, Vector3f.ZERO, rotation);
        setPlayerList(false);

        //Set bounding box to almost nothing so the skull is able to be broken and not cause entity to cast a shadow
        metadata.clear();
        metadata.put(EntityData.SCALE, 1.08f);
        metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.001f);
        metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.001f);
        metadata.getOrCreateFlags().setFlag(EntityFlag.CAN_SHOW_NAME, false);
        metadata.getFlags().setFlag(EntityFlag.INVISIBLE, true); // Until the skin is loaded
    }

    public void despawnEntity(GeyserSession session, Vector3i position) {
        this.despawnEntity(session);
        session.getSkullCache().remove(position, this);
    }
}
