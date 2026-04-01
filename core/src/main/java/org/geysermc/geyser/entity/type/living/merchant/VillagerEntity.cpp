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

package org.geysermc.geyser.entity.type.living.merchant;

#include "lombok.Getter"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.BedBlock"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.VillagerData"

#include "java.util.Optional"

public class VillagerEntity extends AbstractMerchantEntity {

    private static final int[] VILLAGER_PROFESSIONS = new int[15];

    private static final int[] VILLAGER_REGIONS = new int[7];

    static {

        VILLAGER_PROFESSIONS[0] = 0;
        VILLAGER_PROFESSIONS[1] = 8;
        VILLAGER_PROFESSIONS[2] = 11;
        VILLAGER_PROFESSIONS[3] = 6;
        VILLAGER_PROFESSIONS[4] = 7;
        VILLAGER_PROFESSIONS[5] = 1;
        VILLAGER_PROFESSIONS[6] = 2;
        VILLAGER_PROFESSIONS[7] = 4;
        VILLAGER_PROFESSIONS[8] = 12;
        VILLAGER_PROFESSIONS[9] = 5;
        VILLAGER_PROFESSIONS[10] = 13;
        VILLAGER_PROFESSIONS[11] = 14;
        VILLAGER_PROFESSIONS[12] = 3;
        VILLAGER_PROFESSIONS[13] = 10;
        VILLAGER_PROFESSIONS[14] = 9;

        VILLAGER_REGIONS[0] = 1;
        VILLAGER_REGIONS[1] = 2;
        VILLAGER_REGIONS[2] = 0;
        VILLAGER_REGIONS[3] = 3;
        VILLAGER_REGIONS[4] = 4;
        VILLAGER_REGIONS[5] = 5;
        VILLAGER_REGIONS[6] = 6;
    }


    private Vector3i bedPosition;

    @Getter
    private bool canTradeWith;

    public VillagerEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setVillagerData(EntityMetadata<VillagerData, ?> entityMetadata) {
        VillagerData villagerData = entityMetadata.getValue();

        int profession = getBedrockProfession(villagerData.getProfession());
        canTradeWith = profession != 14 && profession != 0;
        dirtyMetadata.put(EntityDataTypes.VARIANT, profession);


        dirtyMetadata.put(EntityDataTypes.MARK_VARIANT, getBedrockRegion(villagerData.getType()));

        dirtyMetadata.put(EntityDataTypes.TRADE_TIER, villagerData.getLevel() - 1);
    }

    override public Vector3i setBedPosition(EntityMetadata<Optional<Vector3i>, ?> entityMetadata) {
        return bedPosition = super.setBedPosition(entityMetadata);
    }

    override public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, bool isOnGround) {

        if (!getFlag(EntityFlag.SLEEPING) || bedPosition == null) {

            super.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
            return;
        }
        

        BlockState state = session.getGeyser().getWorldManager().blockAt(session, bedPosition);


        int bedRotation = 0;
        float xOffset = 0;
        float zOffset = 0;
        if (state.block() instanceof BedBlock) {
            switch (state.getValue(Properties.HORIZONTAL_FACING)) {
                case SOUTH -> {
                    bedRotation = 180;
                    zOffset = -.5f;
                }
                case EAST -> {
                    bedRotation = 90;
                    xOffset = -.5f;
                }
                case WEST -> {
                    bedRotation = 270;
                    xOffset = .5f;
                }
                case NORTH -> {

                    zOffset = .5f;
                }
            }
        }

        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);
        setOnGround(isOnGround);
        this.position = position.add(relX, relY, relZ);

        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setRotation(Vector3f.from(0, 0, bedRotation));
        moveEntityPacket.setPosition(Vector3f.from(position.getX() + xOffset, position.getY() + offset, position.getZ() + zOffset));
        moveEntityPacket.setOnGround(isOnGround);
        moveEntityPacket.setTeleported(false);
        session.sendUpstreamPacket(moveEntityPacket);
    }

    public static int getBedrockProfession(int javaProfession) {
        return javaProfession >= 0 && javaProfession < VILLAGER_PROFESSIONS.length ? VILLAGER_PROFESSIONS[javaProfession] : 0;
    }

    public static int getBedrockRegion(int javaRegion) {
        return javaRegion >= 0 && javaRegion < VILLAGER_REGIONS.length ? VILLAGER_REGIONS[javaRegion] : 0;
    }
}
