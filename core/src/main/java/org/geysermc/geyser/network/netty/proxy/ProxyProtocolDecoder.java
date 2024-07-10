/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.netty.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.ProtocolDetectionResult;
import io.netty.handler.codec.haproxy.*;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * Decodes an HAProxy proxy protocol header
 *
 * @see <a href="https://haproxy.1wt.eu/download/1.5/doc/proxy-protocol.txt">Proxy Protocol Specification</a>
 * @see <a href="https://github.com/netty/netty/blob/4.1/codec-haproxy/src/main/java/io/netty/handler/codec/haproxy/HAProxyMessageDecoder.java">Netty implementation</a>
 */
public final class ProxyProtocolDecoder {
    /**
     * {@link ProtocolDetectionResult} for {@link HAProxyProtocolVersion#V1}.
     */
    private static final ProtocolDetectionResult<HAProxyProtocolVersion> DETECTION_RESULT_V1 =
            ProtocolDetectionResult.detected(HAProxyProtocolVersion.V1);

    /**
     * {@link ProtocolDetectionResult} for {@link HAProxyProtocolVersion#V2}.
     */
    private static final ProtocolDetectionResult<HAProxyProtocolVersion> DETECTION_RESULT_V2 =
            ProtocolDetectionResult.detected(HAProxyProtocolVersion.V2);

    /**
     * Used to extract a header frame out of the {@link ByteBuf} and return it.
     */
    private HeaderExtractor headerExtractor;

    /**
     * {@code true} if we're discarding input because we're already over maxLength
     */
    private boolean discarding;

    /**
     * Number of discarded bytes
     */
    private int discardedBytes;

    /**
     * {@code true} if we're finished decoding the proxy protocol header
     */
    private boolean finished;

    /**
     * Protocol specification version
     */
    private final int decodingVersion;

    /**
     * The latest v2 spec (2014/05/18) allows for additional data to be sent in the proxy protocol header beyond the
     * address information block so now we need a configurable max header size
     */
    private final int v2MaxHeaderSize = 16 + 65535; // TODO: need to calculate max length if TLVs are desired.

    private ProxyProtocolDecoder(int version) {
        this.decodingVersion = version;
    }

    public static @Nullable HAProxyMessage decode(ByteBuf packet, int version) {
        if (version == -1) {
            return null;
        }
        ProxyProtocolDecoder decoder = new ProxyProtocolDecoder(version);
        return decoder.decodeHeader(packet);
    }

    private @Nullable HAProxyMessage decodeHeader(ByteBuf in) {
        final ByteBuf decoded = decodingVersion == 1 ? decodeLine(in) : decodeStruct(in);
        if (decoded == null) {
            return null;
        }

        finished = true;
        try {
            if (decodingVersion == 1) {
                return decodeHeader(decoded.toString(CharsetUtil.US_ASCII));
            } else {
                return decodeHeader0(decoded);
            }
        } catch (HAProxyProtocolException e) {
            throw fail(null, e);
        }
    }

    /**
     * Decodes a version 2, binary proxy protocol header. Copied from HAProxyMessage.
     *
     * @param header                     a version 2 proxy protocol header
     * @return                           {@link HAProxyMessage} instance
     * @throws HAProxyProtocolException  if any portion of the header is invalid
     */
    static HAProxyMessage decodeHeader0(ByteBuf header) {
        Objects.requireNonNull(header, "header");

        if (header.readableBytes() < 16) {
            throw new HAProxyProtocolException(
                    "incomplete header: " + header.readableBytes() + " bytes (expected: 16+ bytes)");
        }

        // Per spec, the 13th byte is the protocol version and command byte
        header.skipBytes(12);
        final byte verCmdByte = header.readByte();

        HAProxyProtocolVersion ver;
        try {
            ver = HAProxyProtocolVersion.valueOf(verCmdByte);
        } catch (IllegalArgumentException e) {
            throw new HAProxyProtocolException(e);
        }

        if (ver != HAProxyProtocolVersion.V2) {
            throw new HAProxyProtocolException("version 1 unsupported: 0x" + Integer.toHexString(verCmdByte));
        }

        HAProxyCommand cmd;
        try {
            cmd = HAProxyCommand.valueOf(verCmdByte);
        } catch (IllegalArgumentException e) {
            throw new HAProxyProtocolException(e);
        }

        if (cmd == HAProxyCommand.LOCAL) {
            return unknownMsg(HAProxyProtocolVersion.V2, HAProxyCommand.LOCAL);
        }

        // Per spec, the 14th byte is the protocol and address family byte
        HAProxyProxiedProtocol protAndFam;
        try {
            protAndFam = HAProxyProxiedProtocol.valueOf(header.readByte());
        } catch (IllegalArgumentException e) {
            throw new HAProxyProtocolException(e);
        }

        if (protAndFam == HAProxyProxiedProtocol.UNKNOWN) {
            return unknownMsg(HAProxyProtocolVersion.V2, HAProxyCommand.PROXY);
        }

        int addressInfoLen = header.readUnsignedShort();

        String srcAddress;
        String dstAddress;
        int addressLen;
        int srcPort = 0;
        int dstPort = 0;

        HAProxyProxiedProtocol.AddressFamily addressFamily = protAndFam.addressFamily();

        if (addressFamily == HAProxyProxiedProtocol.AddressFamily.AF_UNIX) {
            // unix sockets require 216 bytes for address information
            if (addressInfoLen < 216 || header.readableBytes() < 216) {
                throw new HAProxyProtocolException(
                        "incomplete UNIX socket address information: " +
                                Math.min(addressInfoLen, header.readableBytes()) + " bytes (expected: 216+ bytes)");
            }
            int startIdx = header.readerIndex();
            int addressEnd = header.forEachByte(startIdx, 108, ByteProcessor.FIND_NUL);
            if (addressEnd == -1) {
                addressLen = 108;
            } else {
                addressLen = addressEnd - startIdx;
            }
            srcAddress = header.toString(startIdx, addressLen, CharsetUtil.US_ASCII);

            startIdx += 108;

            addressEnd = header.forEachByte(startIdx, 108, ByteProcessor.FIND_NUL);
            if (addressEnd == -1) {
                addressLen = 108;
            } else {
                addressLen = addressEnd - startIdx;
            }
            dstAddress = header.toString(startIdx, addressLen, CharsetUtil.US_ASCII);
            // AF_UNIX defines that exactly 108 bytes are reserved for the address. The previous methods
            // did not increase the reader index although we already consumed the information.
            header.readerIndex(startIdx + 108);
        } else {
            if (addressFamily == HAProxyProxiedProtocol.AddressFamily.AF_IPv4) {
                // IPv4 requires 12 bytes for address information
                if (addressInfoLen < 12 || header.readableBytes() < 12) {
                    throw new HAProxyProtocolException(
                            "incomplete IPv4 address information: " +
                                    Math.min(addressInfoLen, header.readableBytes()) + " bytes (expected: 12+ bytes)");
                }
                addressLen = 4;
            } else if (addressFamily == HAProxyProxiedProtocol.AddressFamily.AF_IPv6) {
                // IPv6 requires 36 bytes for address information
                if (addressInfoLen < 36 || header.readableBytes() < 36) {
                    throw new HAProxyProtocolException(
                            "incomplete IPv6 address information: " +
                                    Math.min(addressInfoLen, header.readableBytes()) + " bytes (expected: 36+ bytes)");
                }
                addressLen = 16;
            } else {
                throw new HAProxyProtocolException(
                        "unable to parse address information (unknown address family: " + addressFamily + ')');
            }

            // Per spec, the src address begins at the 17th byte
            srcAddress = ipBytesToString(header, addressLen);
            dstAddress = ipBytesToString(header, addressLen);
            srcPort = header.readUnsignedShort();
            dstPort = header.readUnsignedShort();
        }

        //noinspection StatementWithEmptyBody
        while (skipNextTLV(header)) {
        }
        return new HAProxyMessage(ver, cmd, protAndFam, srcAddress, dstAddress, srcPort, dstPort);
    }

    /**
     * Convert ip address bytes to string representation. From IPBytesToString
     *
     * @param header     buffer containing ip address bytes
     * @param addressLen number of bytes to read (4 bytes for IPv4, 16 bytes for IPv6)
     * @return           string representation of the ip address
     */
    private static String ipBytesToString(ByteBuf header, int addressLen) {
        StringBuilder sb = new StringBuilder();
        final int ipv4Len = 4;
        final int ipv6Len = 8;
        if (addressLen == ipv4Len) {
            for (int i = 0; i < ipv4Len; i++) {
                sb.append(header.readByte() & 0xff);
                sb.append('.');
            }
        } else {
            for (int i = 0; i < ipv6Len; i++) {
                sb.append(Integer.toHexString(header.readUnsignedShort()));
                sb.append(':');
            }
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * From HAProxyMessage
     */
    private static boolean skipNextTLV(final ByteBuf header) {
        // We need at least 4 bytes for a TLV
        if (header.readableBytes() < 4) {
            return false;
        }

        header.skipBytes(1);
        header.skipBytes(header.readUnsignedShort());
        return true;
    }

    static HAProxyMessage decodeHeader(String header) {
        if (header == null) {
            throw new HAProxyProtocolException("header");
        }

        String[] parts = header.split(" ");
        int numParts = parts.length;

        if (numParts < 2) {
            throw new HAProxyProtocolException(
                    "invalid header: " + header + " (expected: 'PROXY' and proxied protocol values)");
        }

        if (!"PROXY".equals(parts[0])) {
            throw new HAProxyProtocolException("unknown identifier: " + parts[0]);
        }

        HAProxyProxiedProtocol protAndFam;
        try {
            protAndFam = HAProxyProxiedProtocol.valueOf(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new HAProxyProtocolException(e);
        }

        if (protAndFam != HAProxyProxiedProtocol.TCP4 &&
                protAndFam != HAProxyProxiedProtocol.TCP6 &&
                protAndFam != HAProxyProxiedProtocol.UNKNOWN) {
            throw new HAProxyProtocolException("unsupported v1 proxied protocol: " + parts[1]);
        }

        if (protAndFam == HAProxyProxiedProtocol.UNKNOWN) {
            return unknownMsg(HAProxyProtocolVersion.V1, HAProxyCommand.PROXY);
        }

        if (numParts != 6) {
            throw new HAProxyProtocolException("invalid TCP4/6 header: " + header + " (expected: 6 parts)");
        }

        try {
            return new HAProxyMessage(
                    HAProxyProtocolVersion.V1, HAProxyCommand.PROXY,
                    protAndFam, parts[2], parts[3], portStringToInt(parts[4]), portStringToInt(parts[5]));
        } catch (RuntimeException e) {
            throw new HAProxyProtocolException("invalid HAProxy message", e);
        }
    }

    private static int portStringToInt(String value) {
        int port;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid port: " + value, e);
        }

        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("invalid port: " + value + " (expected: 1 ~ 65535)");
        }

        return port;
    }

    /**
     * Proxy protocol message for 'UNKNOWN' proxied protocols. Per spec, when the proxied protocol is
     * 'UNKNOWN' we must discard all other header values.
     */
    private static HAProxyMessage unknownMsg(HAProxyProtocolVersion version, HAProxyCommand command) {
        return new HAProxyMessage(version, command, HAProxyProxiedProtocol.UNKNOWN, null, null, 0, 0);
    }

    static final byte[] BINARY_PREFIX = {
            (byte) 0x0D,
            (byte) 0x0A,
            (byte) 0x0D,
            (byte) 0x0A,
            (byte) 0x00,
            (byte) 0x0D,
            (byte) 0x0A,
            (byte) 0x51,
            (byte) 0x55,
            (byte) 0x49,
            (byte) 0x54,
            (byte) 0x0A
    };
    static final int BINARY_PREFIX_LENGTH = BINARY_PREFIX.length;

    public static int findVersion(final ByteBuf buffer) {
        final int n = buffer.readableBytes();
        // per spec, the version number is found in the 13th byte
        if (n < 13) {
            return -1;
        }

        int idx = buffer.readerIndex();
        return match(BINARY_PREFIX, buffer, idx) ? buffer.getByte(idx + BINARY_PREFIX_LENGTH) : 1;
    }

    /**
     * Create a frame out of the {@link ByteBuf} and return it.
     *
     * @param buffer  the {@link ByteBuf} from which to read data
     * @return frame  the {@link ByteBuf} which represent the frame or {@code null} if no frame could
     *                be created
     */
    private ByteBuf decodeStruct(ByteBuf buffer) {
        if (headerExtractor == null) {
            headerExtractor = new StructHeaderExtractor(v2MaxHeaderSize);
        }
        return headerExtractor.extract(buffer);
    }

    /**
     * Create a frame out of the {@link ByteBuf} and return it.
     *
     * @param buffer  the {@link ByteBuf} from which to read data
     * @return frame  the {@link ByteBuf} which represent the frame or {@code null} if no frame could
     *                be created
     */
    private ByteBuf decodeLine(ByteBuf buffer) {
        if (headerExtractor == null) {
            headerExtractor = new LineHeaderExtractor(108);
        }
        return headerExtractor.extract(buffer);
    }

    private void failOverLimit(String length) {
        int maxLength = decodingVersion == 1 ? 108 : v2MaxHeaderSize;
        throw fail("header length (" + length + ") exceeds the allowed maximum (" + maxLength + ')', null);
    }

    private HAProxyProtocolException fail(String errMsg, Exception e) {
        finished = true;
        HAProxyProtocolException ppex;
        if (errMsg != null && e != null) {
            ppex = new HAProxyProtocolException(errMsg, e);
        } else if (errMsg != null) {
            ppex = new HAProxyProtocolException(errMsg);
        } else if (e != null) {
            ppex = new HAProxyProtocolException(e);
        } else {
            ppex = new HAProxyProtocolException();
        }
        return ppex;
    }

    static final byte[] TEXT_PREFIX = {
            (byte) 'P',
            (byte) 'R',
            (byte) 'O',
            (byte) 'X',
            (byte) 'Y',
    };

    /**
     * Returns the {@link ProtocolDetectionResult} for the given {@link ByteBuf}.
     */
    public static ProtocolDetectionResult<HAProxyProtocolVersion> detectProtocol(ByteBuf buffer) {
        if (buffer.readableBytes() < 12) {
            return ProtocolDetectionResult.needsMoreData();
        }

        int idx = buffer.readerIndex();

        if (match(BINARY_PREFIX, buffer, idx)) {
            return DETECTION_RESULT_V2;
        }
        if (match(TEXT_PREFIX, buffer, idx)) {
            return DETECTION_RESULT_V1;
        }
        return ProtocolDetectionResult.invalid();
    }

    private static boolean match(byte[] prefix, ByteBuf buffer, int idx) {
        for (int i = 0; i < prefix.length; i++) {
            final byte b = buffer.getByte(idx + i);
            if (b != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * HeaderExtractor create a header frame out of the {@link ByteBuf}.
     */
    private abstract class HeaderExtractor {
        /** Header max size */
        private final int maxHeaderSize;

        protected HeaderExtractor(int maxHeaderSize) {
            this.maxHeaderSize = maxHeaderSize;
        }

        /**
         * Create a frame out of the {@link ByteBuf} and return it.
         *
         * @param buffer  the {@link ByteBuf} from which to read data
         * @return frame  the {@link ByteBuf} which represent the frame or {@code null} if no frame could
         *                be created
         */
        public @Nullable ByteBuf extract(ByteBuf buffer) {
            final int eoh = findEndOfHeader(buffer);
            if (!discarding) {
                if (eoh >= 0) {
                    final int length = eoh - buffer.readerIndex();
                    if (length > maxHeaderSize) {
                        buffer.readerIndex(eoh + delimiterLength(buffer, eoh));
                        failOverLimit(String.valueOf(length));
                        return null;
                    }
                    ByteBuf frame = buffer.readSlice(length);
                    buffer.skipBytes(delimiterLength(buffer, eoh));
                    return frame;
                } else {
                    final int length = buffer.readableBytes();
                    if (length > maxHeaderSize) {
                        discardedBytes = length;
                        buffer.skipBytes(length);
                        discarding = true;
                        failOverLimit("over " + discardedBytes);
                    }
                    return null;
                }
            } else {
                if (eoh >= 0) {
                    final int length = discardedBytes + eoh - buffer.readerIndex();
                    buffer.readerIndex(eoh + delimiterLength(buffer, eoh));
                    discardedBytes = 0;
                    discarding = false;
                    failOverLimit("over " + length);
                } else {
                    discardedBytes += buffer.readableBytes();
                    buffer.skipBytes(buffer.readableBytes());
                }
                return null;
            }
        }

        /**
         * Find the end of the header from the given {@link ByteBuf}，the end may be a CRLF, or the length given by the
         * header.
         *
         * @param buffer the buffer to be searched
         * @return {@code -1} if can not find the end, otherwise return the buffer index of end
         */
        protected abstract int findEndOfHeader(ByteBuf buffer);

        /**
         * Get the length of the header delimiter.
         *
         * @param buffer the buffer where delimiter is located
         * @param eoh index of delimiter
         * @return length of the delimiter
         */
        protected abstract int delimiterLength(ByteBuf buffer, int eoh);
    }

    private final class LineHeaderExtractor extends HeaderExtractor {

        LineHeaderExtractor(int maxHeaderSize) {
            super(maxHeaderSize);
        }

        /**
         * Returns the index in the buffer of the end of line found.
         * Returns -1 if no end of line was found in the buffer.
         */
        @Override
        protected int findEndOfHeader(ByteBuf buffer) {
            final int n = buffer.writerIndex();
            for (int i = buffer.readerIndex(); i < n; i++) {
                final byte b = buffer.getByte(i);
                if (b == '\r' && i < n - 1 && buffer.getByte(i + 1) == '\n') {
                    return i;  // \r\n
                }
            }
            return -1;  // Not found.
        }

        @Override
        protected int delimiterLength(ByteBuf buffer, int eoh) {
            return buffer.getByte(eoh) == '\r' ? 2 : 1;
        }
    }

    private final class StructHeaderExtractor extends HeaderExtractor {

        StructHeaderExtractor(int maxHeaderSize) {
            super(maxHeaderSize);
        }

        /**
         * Returns the index in the buffer of the end of header if found.
         * Returns -1 if no end of header was found in the buffer.
         */
        @Override
        protected int findEndOfHeader(ByteBuf buffer) {
            final int n = buffer.readableBytes();

            // per spec, the 15th and 16th bytes contain the address length in bytes
            if (n < 16) {
                return -1;
            }

            int offset = buffer.readerIndex() + 14;

            // the total header length will be a fixed 16 byte sequence + the dynamic address information block
            int totalHeaderBytes = 16 + buffer.getUnsignedShort(offset);

            // ensure we actually have the full header available
            if (n >= totalHeaderBytes) {
                return totalHeaderBytes;
            } else {
                return -1;
            }
        }

        @Override
        protected int delimiterLength(ByteBuf buffer, int eoh) {
            return 0;
        }
    }
}
