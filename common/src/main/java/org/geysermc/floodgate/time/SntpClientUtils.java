/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.time;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/*
 * Thanks:
 * https://datatracker.ietf.org/doc/html/rfc1769
 * https://github.com/jonsagara/SimpleNtpClient
 * https://stackoverflow.com/a/29138806
 */
public final class SntpClientUtils {
    private static final int NTP_PORT = 123;

    private static final int NTP_PACKET_SIZE = 48;
    private static final int NTP_MODE = 3; // client
    private static final int NTP_VERSION = 3;
    private static final int RECEIVE_TIME_POSITION = 32;

    private static final long NTP_TIME_OFFSET = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    public static long requestTimeOffset(String host, int timeout) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeout);

            InetAddress address = InetAddress.getByName(host);

            ByteBuffer buff = ByteBuffer.allocate(NTP_PACKET_SIZE);

            DatagramPacket request = new DatagramPacket(
                    buff.array(), NTP_PACKET_SIZE, address, NTP_PORT
            );

            // mode is in the least signification 3 bits
            // version is in bits 3-5
            buff.put((byte) (NTP_MODE | (NTP_VERSION << 3)));

            long originateTime = System.currentTimeMillis();
            socket.send(request);

            DatagramPacket response = new DatagramPacket(buff.array(), NTP_PACKET_SIZE);
            socket.receive(response);

            long responseTime = System.currentTimeMillis();

            // everything before isn't important for us
            buff.position(RECEIVE_TIME_POSITION);

            long receiveTime = readTimestamp(buff);
            long transmitTime = readTimestamp(buff);

            return ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2;
        } catch (Exception ignored) {
        }
        return Long.MIN_VALUE;
    }

    private static long readTimestamp(ByteBuffer buffer) {
        //todo look into the ntp 2036 problem
        long seconds = buffer.getInt() & 0xffffffffL;
        long fraction = buffer.getInt() & 0xffffffffL;
        return ((seconds - NTP_TIME_OFFSET) * 1000) + ((fraction * 1000) / 0x100000000L);
    }
}
