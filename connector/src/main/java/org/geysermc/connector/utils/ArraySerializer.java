package org.geysermc.connector.utils;

import java.lang.reflect.Array;

import java.util.List;

import java.util.function.BiConsumer;

import java.util.function.Consumer;

import java.util.function.Function;

import java.util.function.ToIntFunction;



import io.netty.buffer.ByteBuf;



public class ArraySerializer {



    public static ByteBuf readShortByteArraySlice(ByteBuf from, int limit) {

        int length = from.readShort();

        MiscSerializer.checkLimit(length, limit);

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

        return MiscSerializer.readBytes(from, VarNumberSerializer.readVarInt(from));

    }



    public static ByteBuf readVarIntByteArraySlice(ByteBuf from, int limit) {

        int length = VarNumberSerializer.readVarInt(from);

        MiscSerializer.checkLimit(length, limit);

        return from.readSlice(length);

    }



    public static ByteBuf readVarIntByteArraySlice(ByteBuf from) {

        return from.readSlice(VarNumberSerializer.readVarInt(from));

    }



    @SuppressWarnings("unchecked")

    public static <T> T[] readVarIntTArray(ByteBuf from, Class<T> tclass, Function<ByteBuf, T> elementReader) {

        T[] array = (T[]) Array.newInstance(tclass, VarNumberSerializer.readVarInt(from));

        for (int i = 0; i < array.length; i++) {

            array[i] = elementReader.apply(from);

        }

        return array;

    }



    public static int[] readVarIntVarIntArray(ByteBuf from) {

        int[] array = new int[VarNumberSerializer.readVarInt(from)];

        for (int i = 0; i < array.length; i++) {

            array[i] = VarNumberSerializer.readVarInt(from);

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

        MiscSerializer.writeLengthPrefixedBytes(to, (lTo, length) -> lTo.writeShort(length), dataWriter);

    }



    public static <T> void writeShortTArray(ByteBuf to, T[] array, BiConsumer<ByteBuf, T> elementWriter) {

        to.writeShort(array.length);

        for (T element : array) {

            elementWriter.accept(to, element);

        }

    }





    public static void writeVarIntByteArray(ByteBuf to, ByteBuf data) {

        VarNumberSerializer.writeVarInt(to, data.readableBytes());

        to.writeBytes(data);

    }



    public static void writeVarIntByteArray(ByteBuf to, byte[] data) {

        VarNumberSerializer.writeVarInt(to, data.length);

        to.writeBytes(data);

    }



    public static void writeVarIntByteArray(ByteBuf to, Consumer<ByteBuf> dataWriter) {

        MiscSerializer.writeLengthPrefixedBytes(to, VarNumberSerializer::writeFixedSizeVarInt, dataWriter);

    }



    public static void writeVarIntTArray(ByteBuf to, ToIntFunction<ByteBuf> arrayWriter) {

        MiscSerializer.writeSizePrefixedData(to, VarNumberSerializer::writeFixedSizeVarInt, arrayWriter);

    }



    public static <T> void writeVarIntTArray(ByteBuf to, T[] array, BiConsumer<ByteBuf, T> elementWriter) {

        VarNumberSerializer.writeVarInt(to, array.length);

        for (T element : array) {

            elementWriter.accept(to, element);

        }

    }



    public static <T> void writeVarIntTArray(ByteBuf to, List<T> array, BiConsumer<ByteBuf, T> elementWriter) {

        VarNumberSerializer.writeVarInt(to, array.size());

        for (T element : array) {

            elementWriter.accept(to, element);

        }

    }




    public static void writeVarIntVarIntArray(ByteBuf to, int[] array) {

        VarNumberSerializer.writeVarInt(to, array.length);

        for (int element : array) {

            VarNumberSerializer.writeVarInt(to, element);

        }

    }



    public static void writeSVarIntSVarIntArray(ByteBuf to, int[] array) {

        VarNumberSerializer.writeSVarInt(to, array.length);

        for (int element : array) {

            VarNumberSerializer.writeSVarInt(to, element);

        }

    }



    public static void writeVarIntLongArray(ByteBuf to, long[] array) {

        VarNumberSerializer.writeVarInt(to, array.length);

        for (long element : array) {

            to.writeLong(element);

        }

    }



}
