package org.geysermc.connector.utils;

import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3d;
import com.nukkitx.math.vector.Vector3i;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.*;

public class GeyserUtils {
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
        out.writeBytes(new byte[4096 + 4096]);
    }

    public static void skipPosition(ByteBuf from) {
        from.skipBytes(Long.BYTES);
    }

    public static Vector3d readPosition(ByteBuf from) {
        long l = from.readLong();
        return Vector3d.from(
                (int) (l >> 38), (int) (l & 0xFFF), (int) ((l << 26) >> 38)
        );
    }

    public static void readPEPosition(ByteBuf from) {
        readSVarInt(from);
        readVarInt(from);
        readSVarInt(from);
    }

    public static Vector3d readLegacyPositionI(ByteBuf from) {
        return Vector3d.from(from.readInt(), from.readInt(), from.readInt());
    }

    public static void writePosition(ByteBuf to, Vector3i position) {
        to.writeLong(((position.getX() & 0x3FFFFFFL) << 38) | ((position.getZ() & 0x3FFFFFFL) << 12) | (position.getY() & 0xFFFL));
    }

    public static void writeLegacyPositionL(ByteBuf to, Vector3d position) {
        to.writeLong((((int) position.getX() & 0x3FFFFFFL) << 38) | (((int) position.getY() & 0xFFFL) << 26) | ((int) position.getZ() & 0x3FFFFFFL));
    }

    public static void writePEPosition(ByteBuf to, Vector3d position) {
        writeSVarInt(to, (int) position.getX());
        writeVarInt(to, (int) position.getY());
        writeSVarInt(to, (int) position.getZ());
    }

    public static void writeLegacyPositionB(ByteBuf to, Vector3d position) {
        to.writeInt((int) position.getX());
        to.writeByte((int) position.getY());
        to.writeInt((int) position.getZ());
    }

    public static void writeLegacyPositionS(ByteBuf to, Vector3d position) {
        to.writeInt((int) position.getX());
        to.writeShort((int) position.getY());
        to.writeInt((int) position.getZ());
    }

    public static void writeLegacyPositionI(ByteBuf to, Vector3d position) {
        to.writeInt((int) position.getX());
        to.writeInt((int) position.getY());
        to.writeInt((int) position.getZ());
    }

    public static Vector2i readIntChunkCoord(ByteBuf from) {
        return Vector2i.from(from.readInt(), from.readInt());
    }

    public static Vector2i readVarIntChunkCoord(ByteBuf from) {
        return Vector2i.from(readVarInt(from), readVarInt(from));
    }

    public static void writeIntChunkCoord(ByteBuf to, Vector2i chunk) {
        to.writeInt(chunk.getX());
        to.writeInt(chunk.getY());
    }

    public static Vector2i readPEChunkCoord(ByteBuf from) {
        return Vector2i.from(readSVarInt(from), readSVarInt(from));
    }

    public static void writePEChunkCoord(ByteBuf to, Vector2i chunk) {
        writeSVarInt(to, chunk.getX());
        writeSVarInt(to, chunk.getY());
    }

    public static int readLocalCoord(ByteBuf from) {
        return from.readUnsignedShort();
    }

    public static void writeLocalCoord(ByteBuf to, int coord) {
        to.writeShort(coord);
    }

    public static void writeVarIntChunkCoord(ByteBuf to, Vector2i chunk) {
        writeVarInt(to, chunk.getX());
        writeVarInt(to, chunk.getY());
    }

    public static final int MAX_VARINT_LENGTH = 5;

    public static void writeFixedSizeVarInt(ByteBuf to, int i) {
        int writerIndex = to.writerIndex();
        while ((i & 0xFFFFFF80) != 0x0) {
            to.writeByte(i | 0x80);
            i >>>= 7;

        }
        int paddingBytes = MAX_VARINT_LENGTH - (to.writerIndex() - writerIndex) - 1;

        if (paddingBytes == 0) {
            to.writeByte(i);
        } else {
            to.writeByte(i | 0x80);
            while (--paddingBytes > 0) {
                to.writeByte(0x80);
            }
            to.writeByte(0);
        }
    }

    public static int readVarInt(ByteBuf from) {
        int value = 0;
        int length = 0;

        byte part;
        do {
            part = from.readByte();
            value |= (part & 0x7F) << (length++ * 7);
            if (length > MAX_VARINT_LENGTH) {
                throw new DecoderException("VarInt too big");
            }
        } while (part < 0);
        return value;
    }

    public static void writeVarInt(ByteBuf to, int i) {
        while ((i & 0xFFFFFF80) != 0x0) {
            to.writeByte(i | 0x80);
            i >>>= 7;
        }
        to.writeByte(i);
    }

    public static int readSVarInt(ByteBuf from) {
        int varint = readVarInt(from);
        return (varint >> 1) ^ -(varint & 1);
    }


    public static void writeSVarInt(ByteBuf to, int varint) {
        writeVarInt(to, (varint << 1) ^ (varint >> 31));
    }

    public static long readVarLong(ByteBuf from) {
        long varlong = 0L;
        int length = 0;

        byte part;
        do {
            part = from.readByte();
            varlong |= (part & 0x7F) << (length++ * 7);

            if (length > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while ((part & 0x80) == 0x80);
        return varlong;
    }

    public static void writeVarLong(ByteBuf to, long varlong) {
        while ((varlong & 0xFFFFFFFFFFFFFF80L) != 0x0L) {
            to.writeByte((int) (varlong & 0x7FL) | 0x80);
            varlong >>>= 7;
        }
        to.writeByte((int) varlong);
    }

    public static long readSVarLong(ByteBuf from) {
        long varlong = readVarLong(from);
        return (varlong >> 1) ^ -(varlong & 1);
    }


    public static void writeSVarLong(ByteBuf to, long varlong) {
        writeVarLong(to, (varlong << 1) ^ (varlong >> 63));

    }

    public static ByteBuf readShortByteArraySlice(ByteBuf from, int limit) {
        int length = from.readShort();
        checkLimit(length, limit);
        return from.readSlice(length);

    }

    @SuppressWarnings("unchecked")
    public static <T> T[] readShortTArray(ByteBuf from, Class<T> tclass, Function<ByteBuf, T> elementReader) {
        T[] array = (T[]) Array.newInstance(tclass, from.readShort());
        for (int i = 0; i < array.length; i++) {
            array[i] = elementReader.apply(from);
        }
        return array;
    }

    public static byte[] readVarIntByteArray(ByteBuf from) {
        return readBytes(from, readVarInt(from));
    }

    public static ByteBuf readVarIntByteArraySlice(ByteBuf from, int limit) {
        int length = readVarInt(from);
        checkLimit(length, limit);
        return from.readSlice(length);
    }

    public static ByteBuf readVarIntByteArraySlice(ByteBuf from) {
        return from.readSlice(readVarInt(from));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] readVarIntTArray(ByteBuf from, Class<T> tclass, Function<ByteBuf, T> elementReader) {
        T[] array = (T[]) Array.newInstance(tclass, readVarInt(from));
        for (int i = 0; i < array.length; i++) {
            array[i] = elementReader.apply(from);
        }
        return array;
    }

    public static int[] readVarIntVarIntArray(ByteBuf from) {
        int[] array = new int[readVarInt(from)];
        for (int i = 0; i < array.length; i++) {
            array[i] = readVarInt(from);
        }
        return array;
    }

    public static void writeShortByteArray(ByteBuf to, ByteBuf data) {
        to.writeShort(data.readableBytes());
        to.writeBytes(data);
    }

    public static void writeShortByteArray(ByteBuf to, byte[] data) {
        to.writeShort(data.length);
        to.writeBytes(data);
    }

    public static void writeShortByteArray(ByteBuf to, Consumer<ByteBuf> dataWriter) {
        writeLengthPrefixedBytes(to, ByteBuf::writeShort, dataWriter);
    }


    public static <T> void writeShortTArray(ByteBuf to, T[] array, BiConsumer<ByteBuf, T> elementWriter) {
        to.writeShort(array.length);
        for (T element : array) {
            elementWriter.accept(to, element);
        }
    }

    public static void writeVarIntByteArray(ByteBuf to, ByteBuf data) {
        writeVarInt(to, data.readableBytes());
        to.writeBytes(data);
    }


    public static void writeVarIntByteArray(ByteBuf to, byte[] data) {
        writeVarInt(to, data.length);
        to.writeBytes(data);
    }

    public static void writeVarIntByteArray(ByteBuf to, Consumer<ByteBuf> dataWriter) {
        writeLengthPrefixedBytes(to, GeyserUtils::writeFixedSizeVarInt, dataWriter);
    }

    public static void writeVarIntTArray(ByteBuf to, ToIntFunction<ByteBuf> arrayWriter) {
        writeSizePrefixedData(to, GeyserUtils::writeFixedSizeVarInt, arrayWriter);
    }

    public static <T> void writeVarIntTArray(ByteBuf to, T[] array, BiConsumer<ByteBuf, T> elementWriter) {
        writeVarInt(to, array.length);
        for (T element : array) {
            elementWriter.accept(to, element);
        }
    }

    public static <T> void writeVarIntTArray(ByteBuf to, List<T> array, BiConsumer<ByteBuf, T> elementWriter) {
        writeVarInt(to, array.size());
        for (T element : array) {
            elementWriter.accept(to, element);
        }
    }

    public static void writeVarIntEnum(ByteBuf to, Enum<?> e) {
        writeVarInt(to, e.ordinal());
    }

    public static void writeByteEnum(ByteBuf to, Enum<?> e) {
        to.writeByte(e.ordinal());
    }

    public static UUID readUUID(ByteBuf from) {
        return new UUID(from.readLong(), from.readLong());
    }

    public static void writeUUID(ByteBuf to, UUID uuid) {
        to.writeLong(uuid.getMostSignificantBits());
        to.writeLong(uuid.getLeastSignificantBits());
    }

    public static void writePEUUID(ByteBuf to, UUID uuid) {
        to.writeLongLE(uuid.getMostSignificantBits());
        to.writeLongLE(uuid.getLeastSignificantBits());
    }

    public static byte[] readAllBytes(ByteBuf buf) {
        return readBytes(buf, buf.readableBytes());
    }

    public static ByteBuf readAllBytesSlice(ByteBuf from) {
        return from.readSlice(from.readableBytes());
    }

    public static ByteBuf readAllBytesSlice(ByteBuf buf, int limit) {
        checkLimit(buf.readableBytes(), limit);
        return readAllBytesSlice(buf);
    }

    public static byte[] readBytes(ByteBuf buf, int length) {
        byte[] result = new byte[length];
        buf.readBytes(result);
        return result;
    }

    protected static void checkLimit(int length, int limit) {
        if (length > limit) {
            throw new DecoderException(MessageFormat.format("Size {0} is bigger than allowed {1}", length, limit));
        }
    }

    public static void writeLengthPrefixedBytes(ByteBuf to, ObjIntConsumer<ByteBuf> lengthWriter, Consumer<ByteBuf> dataWriter) {
        int lengthWriterIndex = to.writerIndex();
        lengthWriter.accept(to, 0);
        int writerIndexDataStart = to.writerIndex();
        dataWriter.accept(to);
        int writerIndexDataEnd = to.writerIndex();
        to.writerIndex(lengthWriterIndex);
        lengthWriter.accept(to, writerIndexDataEnd - writerIndexDataStart);
        to.writerIndex(writerIndexDataEnd);
    }

    public static void writeSizePrefixedData(ByteBuf to, ObjIntConsumer<ByteBuf> sizeWriter, ToIntFunction<ByteBuf> dataWriter) {
        int sizeWriterIndex = to.writerIndex();
        sizeWriter.accept(to, 0);
        int size = dataWriter.applyAsInt(to);
        int writerIndexDataEnd = to.writerIndex();
        to.writerIndex(sizeWriterIndex);
        sizeWriter.accept(to, size);
        to.writerIndex(writerIndexDataEnd);
    }


    private static int getAnvilIndex(int x, int y, int z) {
        return (y << 8) + (z << 4) + x;
    }

    public static <T> boolean instanceOf(Class<T> clazz, Object o) {
        try {
            T t = (T) o;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
