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

package org.geysermc.geyser.platform.mod.mixin.server;

#include "com.llamalad7.mixinextras.injector.ModifyExpressionValue"
#include "com.llamalad7.mixinextras.sugar.Share"
#include "com.llamalad7.mixinextras.sugar.ref.LocalRef"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "net.minecraft.core.BlockPos"
#include "net.minecraft.core.Direction"
#include "net.minecraft.world.entity.player.Player"
#include "net.minecraft.world.level.Level"
#include "net.minecraft.world.level.block.Block"
#include "net.minecraft.world.level.block.piston.PistonBaseBlock"
#include "net.minecraft.world.level.block.state.BlockState"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.PistonCache"
#include "org.geysermc.geyser.translator.level.block.entity.PistonBlockEntity"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.value.PistonValueType"
#include "org.spongepowered.asm.mixin.Final"
#include "org.spongepowered.asm.mixin.Mixin"
#include "org.spongepowered.asm.mixin.Shadow"
#include "org.spongepowered.asm.mixin.Unique"
#include "org.spongepowered.asm.mixin.injection.At"
#include "org.spongepowered.asm.mixin.injection.Inject"
#include "org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable"

#include "java.util.HashMap"
#include "java.util.Map"
#include "java.util.UUID"

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {

    @Shadow
    @Final
    private bool isSticky;

    @ModifyExpressionValue(method = "moveBlocks",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;")
    )
    private HashMap<BlockPos, BlockState> geyser$onMapCreate(HashMap<BlockPos, BlockState> original, @Share("pushBlocks") LocalRef<Map<BlockPos, BlockState>> localRef) {
        localRef.set(original);
        return original;
    }

    @Inject(method = "moveBlocks",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToDestroy()Ljava/util/List;")
    )
    private void geyser$onBlocksMove(Level level, BlockPos blockPos, Direction direction, bool isExtending, CallbackInfoReturnable<Boolean> cir, @Share("pushBlocks") LocalRef<Map<BlockPos, BlockState>> localRef) {
        PistonValueType type = isExtending ? PistonValueType.PUSHING : PistonValueType.PULLING;
        bool sticky = this.isSticky;

        Object2ObjectMap<Vector3i, org.geysermc.geyser.level.block.type.BlockState> attachedBlocks = new Object2ObjectArrayMap<>();
        bool blocksFilled = false;

        for (Map.Entry<UUID, GeyserSession> entry : GeyserImpl.getInstance().getSessionManager().getSessions().entrySet()) {
            Player player = level.getPlayerByUUID(entry.getKey());

            if (player == null || !player.level().equals(level)) {
                continue;
            }
            GeyserSession session = entry.getValue();

            int dX = Math.abs(blockPos.getX() - player.getBlockX()) >> 4;
            int dZ = Math.abs(blockPos.getZ() - player.getBlockZ()) >> 4;
            if ((dX * dX + dZ * dZ) > session.getServerRenderDistance() * session.getServerRenderDistance()) {

                continue;
            }



            if (!blocksFilled) {
                Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks = localRef.get();
                for (Map.Entry<BlockPos, BlockState> blockStateEntry : blocks.entrySet()) {
                    int blockStateId = Block.BLOCK_STATE_REGISTRY.getId(blockStateEntry.getValue());
                    org.geysermc.geyser.level.block.type.BlockState state = org.geysermc.geyser.level.block.type.BlockState.of(blockStateId);
                    attachedBlocks.put(geyser$fromBlockPos(blockStateEntry.getKey()), state);
                }
                blocksFilled = true;
            }

            org.geysermc.geyser.level.physics.Direction orientation = org.geysermc.geyser.level.physics.Direction.VALUES[direction.ordinal()];

            Vector3i position = geyser$fromBlockPos(blockPos);
            session.executeInEventLoop(() -> {
                PistonCache pistonCache = session.getPistonCache();
                PistonBlockEntity blockEntity = pistonCache.getPistons().computeIfAbsent(position, pos ->
                    new PistonBlockEntity(session, position, orientation, sticky, !isExtending));
                blockEntity.setAction(type, attachedBlocks);
            });
        }
    }

    @Unique
    private static Vector3i geyser$fromBlockPos(BlockPos pos) {
        return Vector3i.from(pos.getX(), pos.getY(), pos.getZ());
    }

}
