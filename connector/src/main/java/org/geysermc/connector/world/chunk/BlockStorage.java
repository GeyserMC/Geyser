package org.geysermc.connector.world.chunk;

import com.nukkitx.network.VarInts;
import gnu.trove.list.array.TIntArrayList;
import io.netty.buffer.ByteBuf;
import org.geysermc.connector.world.GlobalBlockPalette;
import org.geysermc.connector.world.chunk.palette.Palette;
import org.geysermc.connector.world.chunk.palette.PaletteVersion;

/**
 * Adapted from NukkitX: https://github.com/NukkitX/Nukkit
 */
public class BlockStorage {

    private static final int SIZE = 4096;

    private final TIntArrayList ids;
    private Palette palette;

    public BlockStorage() {
        this(PaletteVersion.V2);
    }

    public BlockStorage(PaletteVersion version) {
        this.palette = version.createPalette(SIZE);
        this.ids = new TIntArrayList(16, -1);
        this.ids.add(0); // Air is at the start of every palette.
    }

    private BlockStorage(Palette palette, TIntArrayList ids) {
        this.ids = ids;
        this.palette = palette;
    }

    public synchronized int getFullBlock(int xzy) {
        return this.palette.get(xzy);
    }

    public synchronized void setFullBlock(int index, int legacyId) {
        this.palette.set(index, this.idFor(legacyId));
    }

    public synchronized void writeToNetwork(ByteBuf buffer) {
        buffer.writeByte(getPaletteHeader(palette.getVersion(), true));

        for (int word : palette.getWords()) {
            buffer.writeIntLE(word);
        }

        VarInts.writeUnsignedInt(buffer, ids.size());
        ids.forEach(id -> {
            VarInts.writeUnsignedInt(buffer, id);
            return true;
        });
    }

    public synchronized void writeToStorage(ByteBuf buffer) {
        buffer.writeByte(getPaletteHeader(palette.getVersion(), false));
        for (int word : palette.getWords()) {
            buffer.writeIntLE(word);
        }

        //TODO: Write persistent NBT tags
    }

    private synchronized void onResize(PaletteVersion version) {
        Palette oldPalette = this.palette;
        this.palette = version.createPalette(SIZE);

        for (int i = 0; i < SIZE; i++) {
            this.palette.set(i, oldPalette.get(i));
        }
    }

    private int idFor(int legacyId) {
        int runtimeId = GlobalBlockPalette.getOrCreateRuntimeId(legacyId);
        int index = this.ids.indexOf(runtimeId);
        if (index != -1) {
            return index;
        }

        index = this.ids.size();
        this.ids.add(runtimeId);
        PaletteVersion version = this.palette.getVersion();
        if (index > version.getMaxEntryValue()) {
            PaletteVersion next = version.next();
            if (next != null) {
                this.onResize(next);
            }
        }
        return index;
    }

    private static int getPaletteHeader(PaletteVersion version, boolean runtime) {
        return (version.getVersion() << 1) | (runtime ? 1 : 0);
    }

    public boolean isEmpty() {
        for (int word : this.palette.getWords()) {
            if (word != 0) {
                return false;
            }
        }
        return true;
    }

    public BlockStorage copy() {
        return new BlockStorage(this.palette.copy(), new TIntArrayList(this.ids));
    }
}
