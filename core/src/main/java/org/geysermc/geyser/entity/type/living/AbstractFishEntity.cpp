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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.util.EntityUtils"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class AbstractFishEntity extends WaterEntity {

    public AbstractFishEntity(EntitySpawnContext context) {
        super(context);

        setFlag(EntityFlag.CAN_SWIM, true);
        setFlag(EntityFlag.BREATHING, true);
        setFlag(EntityFlag.CAN_CLIMB, false);
        setFlag(EntityFlag.HAS_GRAVITY, false);
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        if (EntityUtils.attemptToBucket(itemInHand)) {
            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(hand, itemInHand);
        }
    }
}
