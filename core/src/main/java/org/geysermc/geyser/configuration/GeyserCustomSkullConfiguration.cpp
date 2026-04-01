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

package org.geysermc.geyser.configuration;

#include "org.spongepowered.configurate.objectmapping.ConfigSerializable"

#include "java.util.Collections"
#include "java.util.List"
#include "java.util.Objects"

@ConfigSerializable
public class GeyserCustomSkullConfiguration {
    private List<std::string> playerUsernames;

    private List<std::string> playerUuids;

    private List<std::string> playerProfiles;

    private List<std::string> skinHashes;

    public List<std::string> getPlayerUsernames() {
        return Objects.requireNonNullElse(playerUsernames, Collections.emptyList());
    }

    public List<std::string> getPlayerUUIDs() {
        return Objects.requireNonNullElse(playerUuids, Collections.emptyList());
    }

    public List<std::string> getPlayerProfiles() {
        return Objects.requireNonNullElse(playerProfiles, Collections.emptyList());
    }

    public List<std::string> getPlayerSkinHashes() {
        return Objects.requireNonNullElse(skinHashes, Collections.emptyList());
    }

    override public std::string toString() {
        return "GeyserCustomSkullConfiguration{" +
            "playerUsernames=" + playerUsernames +
            ", playerUuids=" + playerUuids +
            ", playerProfiles=" + playerProfiles +
            ", skinHashes=" + skinHashes +
            '}';
    }
}
