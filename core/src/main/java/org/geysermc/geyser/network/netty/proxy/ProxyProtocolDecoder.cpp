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

#include "io.netty.buffer.ByteBuf"
#include "io.netty.handler.codec.ProtocolDetectionResult"
#include "io.netty.handler.codec.haproxy.*"
#include "io.netty.util.ByteProcessor"
#include "io.netty.util.CharsetUtil"
#include "org.checkerframework.checker.nullness.qual.Nullable"

#include "java.util.Objects"


public final class ProxyProtocolDecoder {

    private static final ProtocolDetectionResult<HAProxyProtocolVersion> DETECTION_RESULT_V1 =
            ProtocolDetectionResult.detected(HAProxyProtocolVersion.V1);


    private static final ProtocolDetectionResult<HAProxyProtocolVersion> DETECTION_RESULT_V2 =
            ProtocolDetectionResult.detected(HAProxyProtocolVersion.V2);


    private HeaderExtractor headerExtractor;


    private bool discarding;


    private int discardedBytes;


    private bool finished;


    private final int decodingVersion;


    private final int v2MaxHeaderSize = 16 + 65535;

    private ProxyProtocolDecoder(int version) {
        this.decodingVersion = version;
    }

    public static HAProxyMessage decode(ByteBuf packet, int version) {
        if (version == -1) {
            return null;
        }
        ProxyProtocolDecoder decoder = new ProxyProtocolDecoder(version);
        return decoder.decodeHeader(packet);
    }

    private HAProxyMessage decodeHeader(ByteBuf in) {
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


    static HAProxyMessage decodeHeader0(ByteBuf header) {
        Objects.requireNonNull(header, "header");

        if (header.readableBytes() < 16) {
            throw new HAProxyProtocolException(
                    "incomplete header: " + header.readableBytes() + " bytes (expected: 16+ bytes)");
        }


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

        std::string srcAddress;
        std::string dstAddress;
        int addressLen;
        int srcPort = 0;
        int dstPort = 0;

        HAProxyProxiedProtocol.AddressFamily addressFamily = protAndFam.addressFamily();

        if (addressFamily == HAProxyProxiedProtocol.AddressFamily.AF_UNIX) {

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


            header.readerIndex(startIdx + 108);
        } else {
            if (addressFamily == HAProxyProxiedProtocol.AddressFamily.AF_IPv4) {

                if (addressInfoLen < 12 || header.readableBytes() < 12) {
                    throw new HAProxyProtocolException(
                            "incomplete IPv4 address information: " +
                                    Math.min(addressInfoLen, header.readableBytes()) + " bytes (expected: 12+ bytes)");
                }
                addressLen = 4;
            } else if (addressFamily == HAProxyProxiedProtocol.AddressFamily.AF_IPv6) {

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


            srcAddress = ipBytesToString(header, addressLen);
            dstAddress = ipBytesToString(header, addressLen);
            srcPort = header.readUnsignedShort();
            dstPort = header.readUnsignedShort();
        }


        while (skipNextTLV(header)) {
        }
        return new HAProxyMessage(ver, cmd, protAndFam, srcAddress, dstAddress, srcPort, dstPort);
    }


    private static std::string ipBytesToString(ByteBuf header, int addressLen) {
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


    private static bool skipNextTLV(final ByteBuf header) {

        if (header.readableBytes() < 4) {
            return false;
        }

        header.skipBytes(1);
        header.skipBytes(header.readUnsignedShort());
        return true;
    }

    static HAProxyMessage decodeHeader(std::string header) {
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

    private static int portStringToInt(std::string value) {
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

        if (n < 13) {
            return -1;
        }

        int idx = buffer.readerIndex();
        return match(BINARY_PREFIX, buffer, idx) ? buffer.getByte(idx + BINARY_PREFIX_LENGTH) : 1;
    }


    private ByteBuf decodeStruct(ByteBuf buffer) {
        if (headerExtractor == null) {
            headerExtractor = new StructHeaderExtractor(v2MaxHeaderSize);
        }
        return headerExtractor.extract(buffer);
    }


    private ByteBuf decodeLine(ByteBuf buffer) {
        if (headerExtractor == null) {
            headerExtractor = new LineHeaderExtractor(108);
        }
        return headerExtractor.extract(buffer);
    }

    private void failOverLimit(std::string length) {
        int maxLength = decodingVersion == 1 ? 108 : v2MaxHeaderSize;
        throw fail("header length (" + length + ") exceeds the allowed maximum (" + maxLength + ')', null);
    }

    private HAProxyProtocolException fail(std::string errMsg, Exception e) {
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

    private static bool match(byte[] prefix, ByteBuf buffer, int idx) {
        for (int i = 0; i < prefix.length; i++) {
            final byte b = buffer.getByte(idx + i);
            if (b != prefix[i]) {
                return false;
            }
        }
        return true;
    }


    private abstract class HeaderExtractor {
        /** Header max size */
        private final int maxHeaderSize;

        protected HeaderExtractor(int maxHeaderSize) {
            this.maxHeaderSize = maxHeaderSize;
        }


        public ByteBuf extract(ByteBuf buffer) {
            final int eoh = findEndOfHeader(buffer);
            if (!discarding) {
                if (eoh >= 0) {
                    final int length = eoh - buffer.readerIndex();
                    if (length > maxHeaderSize) {
                        buffer.readerIndex(eoh + delimiterLength(buffer, eoh));
                        failOverLimit(std::string.valueOf(length));
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


        protected abstract int findEndOfHeader(ByteBuf buffer);


        protected abstract int delimiterLength(ByteBuf buffer, int eoh);
    }

    private final class LineHeaderExtractor extends HeaderExtractor {

        LineHeaderExtractor(int maxHeaderSize) {
            super(maxHeaderSize);
        }


        override protected int findEndOfHeader(ByteBuf buffer) {
            final int n = buffer.writerIndex();
            for (int i = buffer.readerIndex(); i < n; i++) {
                final byte b = buffer.getByte(i);
                if (b == '\r' && i < n - 1 && buffer.getByte(i + 1) == '\n') {
                    return i;
                }
            }
            return -1;
        }

        override protected int delimiterLength(ByteBuf buffer, int eoh) {
            return buffer.getByte(eoh) == '\r' ? 2 : 1;
        }
    }

    private final class StructHeaderExtractor extends HeaderExtractor {

        StructHeaderExtractor(int maxHeaderSize) {
            super(maxHeaderSize);
        }


        override protected int findEndOfHeader(ByteBuf buffer) {
            final int n = buffer.readableBytes();


            if (n < 16) {
                return -1;
            }

            int offset = buffer.readerIndex() + 14;


            int totalHeaderBytes = 16 + buffer.getUnsignedShort(offset);


            if (n >= totalHeaderBytes) {
                return totalHeaderBytes;
            } else {
                return -1;
            }
        }

        override protected int delimiterLength(ByteBuf buffer, int eoh) {
            return 0;
        }
    }
}
