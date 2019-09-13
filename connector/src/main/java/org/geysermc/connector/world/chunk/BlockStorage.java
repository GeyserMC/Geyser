package org.geysermc.connector.world.chunk;

import com.nukkitx.network.VarInts;
import gnu.trove.list.array.TIntArrayList;
import io.netty.buffer.ByteBuf;
import org.geysermc.connector.world.GlobalBlockPalette;
import org.geysermc.connector.world.chunk.bitarray.BitArray;
import org.geysermc.connector.world.chunk.bitarray.BitArrayVersion;

/**
 * Adapted from NukkitX: https://github.com/NukkitX/Nukkit
 */
public class BlockStorage {

    private static final int SIZE = 4096;

    private final TIntArrayList palette;
    private BitArray bitArray;

    public BlockStorage() {
        this(BitArrayVersion.V2);
    }

    public BlockStorage(BitArrayVersion version) {
        this.bitArray = version.createPalette(SIZE);
        this.palette = new TIntArrayList(16, -1);
        this.palette.add(0); // Air is at the start of every palette.
    }

    private BlockStorage(BitArray bitArray, TIntArrayList palette) {
        this.palette = palette;
        this.bitArray = bitArray;
    }

    private static int getPaletteHeader(BitArrayVersion version, boolean runtime) {
        return (version.getId() << 1) | (runtime ? 1 : 0);
    }

    private static BitArrayVersion getVersionFromHeader(byte header) {
        return BitArrayVersion.get(header >> 1, true);
    }

    public synchronized int getFullBlock(int index) {
        return this.legacyIdFor(this.bitArray.get(index));
    }

    public synchronized void setFullBlock(int index, int legacyId) {
        int idx = this.idFor(legacyId);
        this.bitArray.set(index, idx);
    }

    public synchronized void writeToNetwork(ByteBuf buffer) {
        buffer.writeByte(getPaletteHeader(bitArray.getVersion(), true));

        for (int word : bitArray.getWords()) {
            buffer.writeIntLE(word);
        }

        VarInts.writeInt(buffer, palette.size());
        palette.forEach(id -> {
            VarInts.writeInt(buffer, id);
            return true;
        });
    }

    private void onResize(BitArrayVersion version) {
        BitArray newBitArray = version.createPalette(SIZE);

        for (int i = 0; i < SIZE; i++) {
            newBitArray.set(i, this.bitArray.get(i));
        }
        this.bitArray = newBitArray;
    }

    private int idFor(int legacyId) {
        int runtimeId = GlobalBlockPalette.getOrCreateRuntimeId(legacyId);
        int index = this.palette.indexOf(runtimeId);
        if (index != -1) {
            return index;
        }

        index = this.palette.size();
        this.palette.add(runtimeId);
        BitArrayVersion version = this.bitArray.getVersion();
        if (index > version.getMaxEntryValue()) {
            BitArrayVersion next = version.next();
            if (next != null) {
                this.onResize(next);
            }
        }
        return index;
    }

    private int legacyIdFor(int index) {
        int runtimeId = this.palette.get(index);
        return GlobalBlockPalette.getLegacyId(runtimeId);
    }

    public boolean isEmpty() {
        if (this.palette.size() == 1) {
            return true;
        }
        for (int word : this.bitArray.getWords()) {
            if (Integer.toUnsignedLong(word) != 0L) {
                return false;
            }
        }
        return true;
    }

    public BlockStorage copy() {
        return new BlockStorage(this.bitArray.copy(), new TIntArrayList(this.palette));
    }
}