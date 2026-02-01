/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.util.version;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;

public enum JavaVersion {
    UNKNOWN(-1),
    /**
     * All versions pre-netty
     */
    LEGACY(0),
    JAVA_1_7_2(4),
    JAVA_1_7_6(5),
    JAVA_1_8(47),
    JAVA_1_9(107),
    JAVA_1_9_1(108),
    JAVA_1_9_2(109),
    JAVA_1_9_4(110),
    JAVA_1_10(210),
    JAVA_1_11(315),
    JAVA_1_11_1(316),
    JAVA_1_12(335),
    JAVA_1_12_1(338),
    JAVA_1_12_2(340),
    JAVA_1_13(393),
    JAVA_1_13_1(401),
    JAVA_1_13_2(404),
    JAVA_1_14(477),
    JAVA_1_14_1(480),
    JAVA_1_14_2(485),
    JAVA_1_14_3(490),
    JAVA_1_14_4(498),
    JAVA_1_15(573),
    JAVA_1_15_1(575),
    JAVA_1_15_2(578),
    JAVA_1_16(735),
    JAVA_1_16_1(736),
    JAVA_1_16_2(751),
    JAVA_1_16_3(753),
    JAVA_1_16_4(754),
    JAVA_1_17(755),
    JAVA_1_17_1(756),
    JAVA_1_18(757),
    JAVA_1_18_2(758),
    JAVA_1_19(759),
    JAVA_1_19_1(760),
    JAVA_1_19_3(761),
    JAVA_1_19_4(762),
    JAVA_1_20(763),
    JAVA_1_20_2(764),
    JAVA_1_20_3(765),
    JAVA_1_20_5(766),
    JAVA_1_21(767),
    JAVA_1_21_2(768),
    JAVA_1_21_4(769),
    JAVA_1_21_5(770),
    JAVA_1_21_6(771),
    JAVA_1_21_7(772),
    JAVA_1_21_9(773),
    JAVA_1_21_11(774);

    private static final ImmutableMap<Integer, JavaVersion> LOOKUP;

    static {
        Map<Integer, JavaVersion> versions = new HashMap<>();
        for (JavaVersion version : values()) {
            versions.put(version.protocolVersion, version);
        }

        LOOKUP = ImmutableMap.copyOf(versions);
    }

    public static @NonNull JavaVersion lookup(int protocolVersion) {
        return LOOKUP.getOrDefault(protocolVersion, UNKNOWN);
    }

    JavaVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public boolean newerOrEqual(JavaVersion other) {
        return protocolVersion >= other.protocolVersion;
    }

    public boolean olderThan(JavaVersion other) {
        return protocolVersion < other.protocolVersion;
    }

    @Getter
    private int protocolVersion;
}
