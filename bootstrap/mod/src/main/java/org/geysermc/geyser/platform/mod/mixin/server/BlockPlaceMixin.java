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

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BlockItem.class)
public class BlockPlaceMixin {

    @Inject(method = "place", locals = LocalCapture.CAPTURE_FAILSOFT, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void geyser$hijackPlaySound(BlockPlaceContext blockPlaceContext, CallbackInfoReturnable<InteractionResult> cir, BlockPlaceContext blockPlaceContext2, BlockState blockState, BlockPos blockPos, Level level, Player player, ItemStack itemStack, BlockState blockState2, SoundType soundType) {
        if (player == null) {
            return;
        }

        GeyserSession session = GeyserImpl.getInstance().connectionByUuid(player.getUUID());
        if (session == null) {
            return;
        }

        Vector3f position = Vector3f.from(
            blockPos.getX(),
            blockPos.getY(),
            blockPos.getZ()
        );

        LevelSoundEventPacket placeBlockSoundPacket = new LevelSoundEventPacket();
        placeBlockSoundPacket.setSound(SoundEvent.PLACE);
        placeBlockSoundPacket.setPosition(position);
        placeBlockSoundPacket.setBabySound(false);
        placeBlockSoundPacket.setExtraData(session.getBlockMappings().getBedrockBlockId(Block.BLOCK_STATE_REGISTRY.getId(blockState2)));
        placeBlockSoundPacket.setIdentifier(":");
        session.sendUpstreamPacket(placeBlockSoundPacket);
        session.setLastBlockPlacePosition(null);
        session.setLastBlockPlaced(null);
    }
}
