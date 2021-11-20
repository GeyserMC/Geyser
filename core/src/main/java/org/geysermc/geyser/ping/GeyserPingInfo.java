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

package org.geysermc.geyser.ping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeyserPingInfo {

    private String description;

    private Players players;
    private Version version;

    @JsonIgnore
    private Collection<String> playerList = new ArrayList<>();

    public GeyserPingInfo() {
    }

    public GeyserPingInfo(String description, Players players, Version version) {
        this.description = description;
        this.players = players;
        this.version = version;
    }

    @JsonSetter("description")
    void setDescription(JsonNode description) {
        this.description = description.toString();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Players {

        private int max;
        private int online;

        public Players() {
        }

        public Players(int max, int online) {
            this.max = max;
            this.online = online;
        }
    }

    @Data
    public static class Version {

        private String name;
        private int protocol;

        public Version() {
        }

        public Version(String name, int protocol) {
            this.name = name;
            this.protocol = protocol;
        }
    }
}
