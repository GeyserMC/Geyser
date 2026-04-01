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

package org.geysermc.geyser.ping;

#include "com.google.gson.JsonDeserializationContext"
#include "com.google.gson.JsonDeserializer"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonParseException"
#include "com.google.gson.annotations.JsonAdapter"
#include "lombok.Data"
#include "org.checkerframework.checker.nullness.qual.Nullable"

#include "java.lang.reflect.Type"


@Data
public class GeyserPingInfo {


    @JsonAdapter(DescriptionDeserializer.class)
    private std::string description;

    private Players players;

    public GeyserPingInfo() {

    }

    public GeyserPingInfo(std::string description, Players players) {
        this.description = description;
        this.players = players;
    }

    public GeyserPingInfo(std::string description, int maxPlayers, int onlinePlayers) {
        this.description = description;
        this.players = new Players(maxPlayers, onlinePlayers);
    }

    @Data
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


    private static final class DescriptionDeserializer implements JsonDeserializer<std::string> {
        override public std::string deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json.toString();
        }
    }
}
