/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.scoreboard.network.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.junit.jupiter.api.Assertions;

public class AssertUtils {
    public static <T> void assertContextEquals(Supplier<? extends T> expected, T actual) {
        if (actual == null) {
            Assertions.fail("Expected another packet! " + expected.get());
        }
        Assertions.assertEquals(expected.get(), actual);
    }

    public static void assertNextPacket(GeyserMockContext context, Supplier<BedrockPacket> expected) {
        assertContextEquals(expected, context.nextPacket());
    }

    public static void assertNextPacketsUnordered(GeyserMockContext context, Supplier<List<BedrockPacket>> expected) {
        List<BedrockPacket> packets = new ArrayList<>(expected.get());
        int expectedSize = packets.size();

        for (int i = 0; i < expectedSize; i++) {
            BedrockPacket actual = context.nextPacket();
            if (actual == null) {
                Assertions.fail("Expected more packets! " + packets);
            }

            boolean matched = false;

            for (Iterator<BedrockPacket> iterator = packets.iterator(); iterator.hasNext(); ) {
                BedrockPacket packet = iterator.next();
                if (packet.equals(actual)) {
                    iterator.remove();
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                Assertions.assertEquals(
                    packets,
                    Collections.singletonList(actual),
                    "Expected packet to match any of the specified packets, but matched none!"
                );
            }
        }
    }

    public static void assertNextPacketType(GeyserMockContext context, Class<? extends BedrockPacket> type) {
        var actual = context.nextPacket();
        if (actual == null) {
            Assertions.fail("Expected another packet! " + type);
        }
        Assertions.assertEquals(type, actual.getClass());
    }

    public static <T extends BedrockPacket> void assertNextPacketMatch(GeyserMockContext context, Class<T> type, Consumer<T> matcher) {
        var actual = context.nextPacket();
        if (actual == null) {
            Assertions.fail("Expected another packet!");
        }
        Assertions.assertEquals(type, actual.getClass(), "Expected packet to be an instance of " + type);
        //noinspection unchecked verified in the line above me
        matcher.accept((T) actual);
    }

    public static <T extends BedrockPacket> void assertNextPacketsMatchUnordered(GeyserMockContext context, Class<T> type, List<Consumer<T>> expected) {
        List<Consumer<T>> packets = new ArrayList<>(expected);
        int expectedSize = packets.size();

        for (int i = 0; i < expectedSize; i++) {
            BedrockPacket actualRaw = context.nextPacket();
            if (actualRaw == null) {
                Assertions.fail("Expected more packets! " + packets);
            }

            Assertions.assertEquals(type, actualRaw.getClass(), "Expected packet to be an instance of " + type);
            T actual = type.cast(actualRaw);

            boolean matched = false;
            List<Throwable> caughtExceptions = new ArrayList<>();

            for (Iterator<Consumer<T>> iterator = packets.iterator(); iterator.hasNext(); ) {
                Consumer<T> packetMatcher = iterator.next();
                try {
                    packetMatcher.accept(actual);
                    iterator.remove();
                    matched = true;
                    break;
                } catch (Throwable exception) {
                    caughtExceptions.add(exception);
                }
            }

            if (!matched) {
                String exceptionsAsString = caughtExceptions.stream().map(Throwable::toString).collect(Collectors.joining("\n"));
                Assertions.fail("Expected packet to match any of the specified packets, but matched none! Mismatched matchers:\n" + exceptionsAsString);
            }
        }
    }

    public static void assertNoNextPacket(GeyserMockContext context) {
        Assertions.assertEquals(
            Collections.emptyList(),
            context.packets(),
            "Expected no remaining packets, got " + context.packetCount()
        );
    }
}
