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

package org.geysermc.geyser.platform.fabric.world;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.geysermc.geyser.level.GeyserWorldManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.LecternInventoryTranslator;
import org.geysermc.geyser.util.BlockEntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GeyserFabricWorldManager extends GeyserWorldManager {
    private final MinecraftServer server;

    public GeyserFabricWorldManager(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public boolean shouldExpectLecternHandled() {
        return true;
    }

    @Override
    public NbtMap getLecternDataAt(GeyserSession session, int x, int y, int z, boolean isChunkLoad) {
        Runnable lecternGet = () -> {
            // Mostly a reimplementation of Spigot lectern support
            ServerPlayer player = getPlayer(session);
            if (player != null) {
                BlockEntity blockEntity = player.level.getBlockEntity(new BlockPos(x, y, z));
                if (!(blockEntity instanceof LecternBlockEntity lectern)) {
                    return;
                }

                if (!lectern.hasBook()) {
                    if (!isChunkLoad) {
                        BlockEntityUtils.updateBlockEntity(session, LecternInventoryTranslator.getBaseLecternTag(x, y, z, 0).build(), Vector3i.from(x, y, z));
                    }
                    return;
                }

                ItemStack book = lectern.getBook();
                int pageCount = WrittenBookItem.getPageCount(book);
                boolean hasBookPages = pageCount > 0;
                NbtMapBuilder lecternTag = LecternInventoryTranslator.getBaseLecternTag(x, y, z, hasBookPages ? pageCount : 1);
                lecternTag.putInt("page", lectern.getPage() / 2);
                NbtMapBuilder bookTag = NbtMap.builder()
                        .putByte("Count", (byte) book.getCount())
                        .putShort("Damage", (short) 0)
                        .putString("Name", "minecraft:writable_book");
                List<NbtMap> pages = new ArrayList<>(hasBookPages ? pageCount : 1);
                if (hasBookPages && WritableBookItem.makeSureTagIsValid(book.getTag())) {
                    ListTag listTag = book.getTag().getList("pages", 8);

                    for (int i = 0; i < listTag.size(); i++) {
                        String page = listTag.getString(i);
                        NbtMapBuilder pageBuilder = NbtMap.builder()
                                .putString("photoname", "")
                                .putString("text", page);
                        pages.add(pageBuilder.build());
                    }
                } else {
                    // Empty page
                    NbtMapBuilder pageBuilder = NbtMap.builder()
                            .putString("photoname", "")
                            .putString("text", "");
                    pages.add(pageBuilder.build());
                }

                bookTag.putCompound("tag", NbtMap.builder().putList("pages", NbtType.COMPOUND, pages).build());
                lecternTag.putCompound("book", bookTag.build());
                NbtMap blockEntityTag = lecternTag.build();
                BlockEntityUtils.updateBlockEntity(session, blockEntityTag, Vector3i.from(x, y, z));
            }
        };
        if (isChunkLoad) {
            // Hacky hacks to allow lectern loading to be delayed
            session.scheduleInEventLoop(() -> server.execute(lecternGet), 1, TimeUnit.SECONDS);
        } else {
            server.execute(lecternGet);
        }
        return LecternInventoryTranslator.getBaseLecternTag(x, y, z, 0).build();
    }

    @Override
    public boolean hasPermission(GeyserSession session, String permission) {
        ServerPlayer player = getPlayer(session);
        return Permissions.check(player, permission);
    }

    private ServerPlayer getPlayer(GeyserSession session) {
        return server.getPlayerList().getPlayer(session.getPlayerEntity().getUuid());
    }
}
