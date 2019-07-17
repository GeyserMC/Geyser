package org.geysermc.connector.utils;

import java.text.MessageFormat;

import java.util.UUID;

import java.util.function.Consumer;

import java.util.function.ObjIntConsumer;

import java.util.function.ToIntFunction;



import io.netty.buffer.ByteBuf;

import io.netty.handler.codec.DecoderException;


public class MiscSerializer {


    public static void writeVarIntEnum(ByteBuf to, Enum<?> e) {

        VarNumberSerializer.writeVarInt(to, e.ordinal());

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

        return MiscSerializer.readBytes(buf, buf.readableBytes());

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



}
