/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class LinkedPlayer implements Cloneable {
    /**
     * The Java username of the linked player
     */
    private final String javaUsername;
    /**
     * The Java UUID of the linked player
     */
    private final UUID javaUniqueId;
    /**
     * The UUID of the Bedrock player
     */
    private final UUID bedrockId;
    /**
     * If the LinkedPlayer is sent from a different platform. For example the LinkedPlayer is from
     * Bungee but the data has been sent to the Bukkit server.
     */
    private boolean fromDifferentPlatform = false;

    public static LinkedPlayer of(String javaUsername, UUID javaUniqueId, UUID bedrockId) {
        return new LinkedPlayer(javaUsername, javaUniqueId, bedrockId);
    }

    public static @Nullable LinkedPlayer fromString(String data) {
        String[] split = data.split(";");
        if (split.length != 3) {
            return null;
        }

        LinkedPlayer player = new LinkedPlayer(
                split[0], UUID.fromString(split[1]), UUID.fromString(split[2])
        );
        player.fromDifferentPlatform = true;
        return player;
    }

    @Override
    public String toString() {
        return javaUsername + ';' + javaUniqueId.toString() + ';' + bedrockId.toString();
    }

    @Override
    public LinkedPlayer clone() throws CloneNotSupportedException {
        return (LinkedPlayer) super.clone();
    }
}
