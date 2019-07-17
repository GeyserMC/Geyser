package org.geysermc.connector.utils;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.netty.buffer.ByteBuf;

public class PositionSerializer {

    public static void skipPosition(ByteBuf from) {
        from.skipBytes(Long.BYTES);
    }

    public static Vector3d readPosition(ByteBuf from) {
        long l = from.readLong();
        return new Vector3d(
            (int) (l >> 38), (int) (l & 0xFFF), (int) ((l << 26) >> 38)
        );
    }

    public static void readPEPosition(ByteBuf from) {
        VarNumberSerializer.readSVarInt(from);
        VarNumberSerializer.readVarInt(from);
        VarNumberSerializer.readSVarInt(from);
    }

    public static Vector3d readLegacyPositionI(ByteBuf from) {
        return new Vector3d(from.readInt(), from.readInt(), from.readInt());
    }

    public static void writePosition(ByteBuf to, Vector3i position) {
        to.writeLong(((position.getX() & 0x3FFFFFFL) << 38) | ((position.getZ() & 0x3FFFFFFL) << 12) | (position.getY() & 0xFFFL));
    }

    public static void writeLegacyPositionL(ByteBuf to, Vector3d position) {
        to.writeLong((((int) position.getX() & 0x3FFFFFFL) << 38) | (((int) position.getY() & 0xFFFL) << 26) | ((int) position.getZ() & 0x3FFFFFFL));
    }

    public static void writePEPosition(ByteBuf to, Vector3d position) {
        VarNumberSerializer.writeSVarInt(to, (int) position.getX());
        VarNumberSerializer.writeVarInt(to, (int) position.getY());
        VarNumberSerializer.writeSVarInt(to, (int) position.getZ());
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
        return new Vector2i(from.readInt(), from.readInt());
    }

    public static Vector2i readVarIntChunkCoord(ByteBuf from) {
        return new Vector2i(VarNumberSerializer.readVarInt(from), VarNumberSerializer.readVarInt(from));
    }

    public static void writeIntChunkCoord(ByteBuf to, Vector2i chunk) {
		to.writeInt(chunk.getX());
		to.writeInt(chunk.getY());
	}

    public static Vector2i readPEChunkCoord(ByteBuf from) {
        return new Vector2i(VarNumberSerializer.readSVarInt(from), VarNumberSerializer.readSVarInt(from));
    }

    public static void writePEChunkCoord(ByteBuf to, Vector2i chunk) {
        VarNumberSerializer.writeSVarInt(to, chunk.getX());
        VarNumberSerializer.writeSVarInt(to, chunk.getY());
    }

    public static int readLocalCoord(ByteBuf from) {
        return from.readUnsignedShort();
    }

    public static void writeLocalCoord(ByteBuf to, int coord) {
        to.writeShort(coord);
    }

    public static void writeVarIntChunkCoord(ByteBuf to, Vector2i chunk) {
        VarNumberSerializer.writeVarInt(to, chunk.getX());
        VarNumberSerializer.writeVarInt(to, chunk.getY());
    }
}