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

package org.geysermc.floodgate.news;

import com.google.gson.JsonArray;
import org.checkerframework.checker.nullness.qual.Nullable;

// {} is used for things that have to be filled in by the server,
// {@} is for things that have to be filled in by us
public enum NewsItemMessage {
    UPDATE_AVAILABLE("There is an update available for {}. The newest version is: {}"),
    UPDATE_RECOMMENDED(UPDATE_AVAILABLE + ". Your version is quite old, updating is recommend."),
    UPDATE_HIGHLY_RECOMMENDED(UPDATE_AVAILABLE + ". We highly recommend updating because some important changes have been made."),
    UPDATE_ANCIENT_VERSION(UPDATE_AVAILABLE + ". You are running an ancient version, updating is recommended."),

    DOWNTIME_GENERIC("The {} is temporarily going down for maintenance soon."),
    DOWNTIME_WITH_START("The {} is temporarily going down for maintenance on {}."),
    DOWNTIME_TIMEFRAME(DOWNTIME_WITH_START + " The maintenance is expected to last till {}.");

    private static final NewsItemMessage[] VALUES = values();

    private final String messageFormat;
    private final String[] messageSplitted;

    NewsItemMessage(String messageFormat) {
        this.messageFormat = messageFormat;
        this.messageSplitted = messageFormat.split(" ");
    }

    public static @Nullable NewsItemMessage getById(int id) {
        return VALUES.length > id ? VALUES[id] : null;
    }

    public String getMessageFormat() {
        return messageFormat;
    }

    public String getFormattedMessage(JsonArray serverArguments) {
        int serverArgumentsIndex = 0;

        StringBuilder message = new StringBuilder();
        for (String split : messageSplitted) {
            if (message.length() > 0) {
                message.append(' ');
            }

            String result = split;

            if (serverArgumentsIndex < serverArguments.size()) {
                String argument = serverArguments.get(serverArgumentsIndex).getAsString();
                result = result.replace("{}", argument);
                if (!result.equals(split)) {
                    serverArgumentsIndex++;
                }
            }

            message.append(result);
        }
        return message.toString();
    }


    @Override
    public String toString() {
        return getMessageFormat();
    }
}
