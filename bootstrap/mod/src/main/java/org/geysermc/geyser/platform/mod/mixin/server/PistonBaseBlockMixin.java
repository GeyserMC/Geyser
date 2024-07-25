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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.PistonCache;
import org.geysermc.geyser.translator.level.block.entity.PistonBlockEntity;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.PistonValueType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {

    @Shadow
    @Final
    private boolean isSticky;

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
    private void geyser$onBlocksMove(Level level, BlockPos blockPos, Direction direction, boolean isExtending, CallbackInfoReturnable<Boolean> cir, @Share("pushBlocks") LocalRef<Map<BlockPos, BlockState>> localRef) {
        PistonValueType type = isExtending ? PistonValueType.PUSHING : PistonValueType.PULLING;
        boolean sticky = this.isSticky;

        Object2ObjectMap<Vector3i, org.geysermc.geyser.level.block.type.BlockState> attachedBlocks = new Object2ObjectArrayMap<>();
        boolean blocksFilled = false;

        for (Map.Entry<UUID, GeyserSession> entry : GeyserImpl.getInstance().getSessionManager().getSessions().entrySet()) {
            Player player = level.getPlayerByUUID(entry.getKey());
            //noinspection resource
            if (player == null || !player.level().equals(level)) {
                continue;
            }
            GeyserSession session = entry.getValue();

            int dX = Math.abs(blockPos.getX() - player.getBlockX()) >> 4;
            int dZ = Math.abs(blockPos.getZ() - player.getBlockZ()) >> 4;
            if ((dX * dX + dZ * dZ) > session.getServerRenderDistance() * session.getServerRenderDistance()) {
                // Ignore pistons outside the player's render distance
                continue;
            }

            // Trying to grab the blocks from the world like other platforms would result in the moving piston block
            // being returned instead.
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
