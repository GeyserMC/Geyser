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
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.geysermc.geyser.level.GeyserWorldManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.LecternInventoryTranslator;
import org.geysermc.geyser.util.BlockEntityUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    @Nonnull
    @Override
    public CompletableFuture<com.github.steveice10.opennbt.tag.builtin.CompoundTag> getPickItemNbt(GeyserSession session, int x, int y, int z, boolean addNbtData) {
        CompletableFuture<com.github.steveice10.opennbt.tag.builtin.CompoundTag> future = new CompletableFuture<>();
        server.execute(() -> {
            ServerPlayer player = getPlayer(session);
            if (player == null) {
                future.complete(null);
                return;
            }

            BlockPos pos = new BlockPos(x, y, z);
            // Don't create a new block entity if invalid
            BlockEntity blockEntity = player.level.getChunkAt(pos).getBlockEntity(pos);
            if (blockEntity instanceof BannerBlockEntity banner) {
                // Potentially exposes other NBT data? But we need to get the NBT data for the banner patterns *and*
                // the banner might have a custom name, both of which a Java client knows and caches
                ItemStack itemStack = banner.getItem();
                var tag = OpenNbtTagVisitor.convert("", itemStack.getOrCreateTag());

                future.complete(tag);
                return;
            }
            future.complete(null);
        });
        return future;
    }

    private ServerPlayer getPlayer(GeyserSession session) {
        return server.getPlayerList().getPlayer(session.getPlayerEntity().getUuid());
    }

    // Future considerations: option to clone; would affect arrays
    private static class OpenNbtTagVisitor implements TagVisitor {
        private String currentKey;
        private final com.github.steveice10.opennbt.tag.builtin.CompoundTag root;
        private com.github.steveice10.opennbt.tag.builtin.Tag currentTag;

        OpenNbtTagVisitor(String key) {
            root = new com.github.steveice10.opennbt.tag.builtin.CompoundTag(key);
        }

        @Override
        public void visitString(StringTag stringTag) {
            currentTag = new com.github.steveice10.opennbt.tag.builtin.StringTag(currentKey, stringTag.getAsString());
        }

        @Override
        public void visitByte(ByteTag byteTag) {
            currentTag = new com.github.steveice10.opennbt.tag.builtin.ByteTag(currentKey, byteTag.getAsByte());
        }

        @Override
        public void visitShort(ShortTag shortTag) {
            currentTag = new com.github.steveice10.opennbt.tag.builtin.ShortTag(currentKey, shortTag.getAsShort());
        }

        @Override
        public void visitInt(IntTag intTag) {
            currentTag = new com.github.steveice10.opennbt.tag.builtin.IntTag(currentKey, intTag.getAsInt());
        }

        @Override
        public void visitLong(LongTag longTag) {
            currentTag = new com.github.steveice10.opennbt.tag.builtin.LongTag(currentKey, longTag.getAsLong());
        }

        @Override
        public void visitFloat(FloatTag floatTag) {
            currentTag = new com.github.steveice10.opennbt.tag.builtin.FloatTag(currentKey, floatTag.getAsFloat());
        }

        @Override
        public void visitDouble(DoubleTag doubleTag) {
            currentTag = new com.github.steveice10.opennbt.tag.builtin.DoubleTag(currentKey, doubleTag.getAsDouble());
        }

        @Override
        public void visitByteArray(ByteArrayTag byteArrayTag) {
            currentTag = new com.github.steveice10.opennbt.tag.builtin.ByteArrayTag(currentKey, byteArrayTag.getAsByteArray());
        }

        @Override
        public void visitIntArray(IntArrayTag intArrayTag) {
            currentTag = new com.github.steveice10.opennbt.tag.builtin.IntArrayTag(currentKey, intArrayTag.getAsIntArray());
        }

        @Override
        public void visitLongArray(LongArrayTag longArrayTag) {
            currentTag = new com.github.steveice10.opennbt.tag.builtin.LongArrayTag(currentKey, longArrayTag.getAsLongArray());
        }

        @Override
        public void visitList(ListTag listTag) {
            var newList = new com.github.steveice10.opennbt.tag.builtin.ListTag(currentKey);
            for (Tag tag : listTag) {
                currentKey = "";
                tag.accept(this);
                newList.add(currentTag);
            }
            currentTag = newList;
        }

        @Override
        public void visitCompound(CompoundTag compoundTag) {
            currentTag = convert(currentKey, compoundTag);
        }

        private static com.github.steveice10.opennbt.tag.builtin.CompoundTag convert(String name, CompoundTag compoundTag) {
            OpenNbtTagVisitor visitor = new OpenNbtTagVisitor(name);
            for (String key : compoundTag.getAllKeys()) {
                visitor.currentKey = key;
                Tag tag = compoundTag.get(key);
                tag.accept(visitor);
                visitor.root.put(visitor.currentTag);
            }
            return visitor.root;
        }

        @Override
        public void visitEnd(EndTag endTag) {
        }
    }
}
