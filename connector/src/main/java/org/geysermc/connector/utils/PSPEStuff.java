package org.geysermc.connector.utils;

import io.netty.buffer.ByteBuf;

public class PSPEStuff {

    public static final int FLAG_RUNTIME = 1;

    public static final int GLOBAL_PALETTE_BITS_PER_BLOCK = 14;

    public static final int SECTION_COUNT_BLOCKS = 16;

    public static final int SECTION_COUNT_LIGHT = 18;

    public static final int BLOCKS_IN_SECTION = 16 * 16 * 16;

    public static final int LIGHT_DATA_LENGTH = BLOCKS_IN_SECTION / 2;

    public static final int EMPTY_SUBCHUNK_BYTES = BLOCKS_IN_SECTION / 8;

    public static final int SUBCHUNK_VERSION = 8;

    public static void writeEmpty(ByteBuf to) {

        to.writeByte(storageHeader(1));

        to.writeZero(EMPTY_SUBCHUNK_BYTES);

    }



    protected static final int storageHeader(int bitsPerBlock) {

        return (bitsPerBlock << 1) | FLAG_RUNTIME;

    }

    public static void writeEmptySubChunk(ByteBuf out) {

        out.writeByte(SUBCHUNK_VERSION);

        out.writeByte(1); //only blockstate storage

        writeEmpty(out);

        VarNumberSerializer.writeSVarInt(out, 1); //Palette size

        VarNumberSerializer.writeSVarInt(out, 0); //Palette: Air

    }
}
