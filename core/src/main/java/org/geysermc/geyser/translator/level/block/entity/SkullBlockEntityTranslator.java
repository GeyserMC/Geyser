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

package org.geysermc.geyser.translator.level.block.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@BlockEntity(type = BlockEntityType.SKULL)
public class SkullBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {

    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        Integer rotation = blockState.getValue(Properties.ROTATION_16);
        if (rotation != null) {
            // Could be a wall skull block otherwise, which has rotation in its Bedrock state
            bedrockNbt.putFloat("Rotation", rotation * 22.5f);
        }
        if (blockState.getValue(Properties.POWERED)) {
            bedrockNbt.putBoolean("MouthMoving", true);
        }
    }

    private static List<GameProfile.Property> parseProperties(List<NbtMap> properties) {
        if (properties == null) {
            return null;
        }
        return properties.stream()
            .map(property -> {
                String name = property.getString("name");
                String value = property.getString("value");
                String signature = property.getString("signature");
                return new GameProfile.Property(name, value, signature);
            })
            .toList();
    }

    public static ResolvableProfile parseResolvableProfile(NbtMap profile) {
        UUID uuid = EntityUtils.uuidFromIntArray(profile.getIntArray("id", null));
        String name = profile.getString("name", null);
        List<GameProfile.Property> properties = parseProperties(profile.getList("properties", NbtType.COMPOUND, null));

        GameProfile partialOrStatic = new GameProfile(uuid, name);
        partialOrStatic.setProperties(properties);
        // Only if all fields are present, then the profile is a static one
        // TODO shorthand constructor in MCPL
        return new ResolvableProfile(partialOrStatic);
    }

    public static @Nullable BlockDefinition translateSkull(GeyserSession session, NbtMap javaNbt, Vector3i blockPosition, BlockState blockState) {
        NbtMap profile = javaNbt.getCompound("profile");
        if (profile.isEmpty()) {
            session.getSkullCache().removeSkull(blockPosition);
            return null;
        }

        CompletableFuture<GameProfile> resolvedFuture = SkinManager.resolveProfile(parseResolvableProfile(profile));
        if (resolvedFuture.isDone()) {
            try {
                SkullCache.Skull skull = session.getSkullCache().putSkull(blockPosition, resolvedFuture.get(), blockState);
                if (skull == null) {
                    session.getGeyser().getLogger().debug("Custom skull with invalid profile: " + blockPosition + " " + resolvedFuture.get());
                    return null;
                }
                return skull.getBlockDefinition();
            } catch (InterruptedException | ExecutionException e) {
                session.getGeyser().getLogger().debug("Failed to acquire textures for custom skull: " + blockPosition + " " + javaNbt);
                if (GeyserImpl.getInstance().config().debugMode()) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        // profile contained a username, so we have to wait for it to be retrieved
        resolvedFuture.whenComplete((resolved, throwable) -> {
            if (throwable != null ) {
                session.getGeyser().getLogger().debug("Failed resolving profile of player head at: " + blockPosition + " " + javaNbt);
                if (GeyserImpl.getInstance().config().debugMode()) {
                    throwable.printStackTrace();
                }
                return;
            }
            session.ensureInEventLoop(() -> putSkull(session, blockPosition, resolved, blockState));
        });

        // We don't have the textures yet, so we can't determine if a custom block was defined for this skull
        return null;
    }

    private static void putSkull(GeyserSession session, Vector3i blockPosition, GameProfile resolved, BlockState blockState) {
        SkullCache.Skull skull = session.getSkullCache().putSkull(blockPosition, resolved, blockState);
        if (skull != null && skull.getBlockDefinition() != null) {
            UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
            updateBlockPacket.setDataLayer(0);
            updateBlockPacket.setBlockPosition(blockPosition);
            updateBlockPacket.setDefinition(skull.getBlockDefinition());
            updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
            updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
            session.sendUpstreamPacket(updateBlockPacket);
        }
    }
}
