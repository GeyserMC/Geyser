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

package org.geysermc.geyser.platform.mod.world;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.level.GeyserWorldManager;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.platform.mod.GeyserModBootstrap;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.BannerPatternLayer;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GeyserModWorldManager extends GeyserWorldManager {

    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();
    private final MinecraftServer server;

    public GeyserModWorldManager(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        // If the protocol version of Geyser and the server are not the
        // same, fallback to the chunk cache. May be able to update this
        // in the future to use ViaVersion however, like Spigot does.
        if (SharedConstants.getCurrentVersion().getProtocolVersion() != GameProtocol.getJavaProtocolVersion()) {
            return super.getBlockAt(session, x, y, z);
        }

        ServerPlayer player = this.getPlayer(session);
        if (player == null) {
            return 0;
        }

        Level level = player.level();
        if (y < level.getMinBuildHeight()) {
            return 0;
        }

        ChunkAccess chunk = level.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            return 0;
        }

        int worldOffset = level.getMinBuildHeight() >> 4;
        int chunkOffset = (y >> 4) - worldOffset;
        if (chunkOffset < chunk.getSections().length) {
            LevelChunkSection section = chunk.getSections()[chunkOffset];
            if (section != null && !section.hasOnlyAir()) {
                return Block.getId(section.getBlockState(x & 15, y & 15, z & 15));
            }
        }

        return 0;
    }

    @Override
    public boolean hasOwnChunkCache() {
        return SharedConstants.getCurrentVersion().getProtocolVersion() == GameProtocol.getJavaProtocolVersion();
    }

    @Override
    public boolean hasPermission(GeyserSession session, String permission) {
        ServerPlayer player = getPlayer(session);
        return GeyserModBootstrap.getInstance().hasPermission(player, permission);
    }

    @Override
    public GameMode getDefaultGameMode(GeyserSession session) {
        return GameMode.byId(server.getDefaultGameType().getId());
    }

    @NonNull
    @Override
    public CompletableFuture<org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents> getPickItemComponents(GeyserSession session, int x, int y, int z, boolean addNbtData) {
        CompletableFuture<org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents> future = new CompletableFuture<>();
        server.execute(() -> {
            ServerPlayer player = getPlayer(session);
            if (player == null) {
                future.complete(null);
                return;
            }

            BlockPos pos = new BlockPos(x, y, z);
            // Don't create a new block entity if invalid
            //noinspection resource - level() is just a getter
            BlockEntity blockEntity = player.level().getChunkAt(pos).getBlockEntity(pos);
            if (blockEntity instanceof BannerBlockEntity banner) {
                // Potentially exposes other NBT data? But we need to get the NBT data for the banner patterns *and*
                // the banner might have a custom name, both of which a Java client knows and caches
                ItemStack itemStack = banner.getItem();

                org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents components =
                        new org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents(new HashMap<>());

                components.put(DataComponentType.DAMAGE, itemStack.getDamageValue());

                Component customName = itemStack.getComponents().get(DataComponents.CUSTOM_NAME);
                if (customName != null) {
                    components.put(DataComponentType.CUSTOM_NAME, toKyoriComponent(customName));
                }

                BannerPatternLayers pattern = itemStack.get(DataComponents.BANNER_PATTERNS);
                if (pattern != null) {
                    components.put(DataComponentType.BANNER_PATTERNS, toPatternList(pattern));
                }

                future.complete(components);
                return;
            }
            future.complete(null);
        });
        return future;
    }

    private ServerPlayer getPlayer(GeyserSession session) {
        return server.getPlayerList().getPlayer(session.getPlayerEntity().getUuid());
    }

    private static net.kyori.adventure.text.Component toKyoriComponent(Component component) {
        String json = Component.Serializer.toJson(component, RegistryAccess.EMPTY);
        return GSON_SERIALIZER.deserializeOr(json, net.kyori.adventure.text.Component.empty());
    }

    private static List<BannerPatternLayer> toPatternList(BannerPatternLayers patternLayers) {
        return patternLayers.layers().stream()
                .map(layer -> {
                    BannerPatternLayer.BannerPattern pattern = new BannerPatternLayer.BannerPattern(
                            MinecraftKey.key(layer.pattern().value().assetId().toString()), layer.pattern().value().translationKey()
                    );
                    return new BannerPatternLayer(Holder.ofCustom(pattern), layer.color().getId());
                })
                .toList();
    }
}
