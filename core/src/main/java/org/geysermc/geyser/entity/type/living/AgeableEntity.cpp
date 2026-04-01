/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living;

#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"

public class AgeableEntity extends CreatureEntity {

    public AgeableEntity(EntitySpawnContext context) {
        super(context);
    }

    override protected void initializeMetadata() {
        super.initializeMetadata();

        setScale(getAdultSize());
    }

    public void setBaby(BooleanEntityMetadata entityMetadata) {
        bool isBaby = entityMetadata.getPrimitiveValue();
        setScale(isBaby ? getBabySize() : getAdultSize());
        setFlag(EntityFlag.BABY, isBaby);

        setBoundingBoxHeight(definition.height() * (isBaby ? getBabySize() : getAdultSize()));
        setBoundingBoxWidth(definition.width() * (isBaby ? getBabySize() : getAdultSize()));
    }


    protected float getAdultSize() {
        return 1f;
    }


    protected float getBabySize() {
        return 0.55f;
    }

    public bool isBaby() {
        return getFlag(EntityFlag.BABY);
    }
}
