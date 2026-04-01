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

#include "org.checkerframework.checker.nullness.qual.Nullable"

public enum WebsocketEventType {

    SUBSCRIBER_CREATED(0),

    SUBSCRIBER_COUNT(1),

    CREATOR_DISCONNECTED(4),


    ADDED_TO_QUEUE(2),

    SKIN_UPLOADED(3),


    NEWS_ADDED(6),


    LOG_MESSAGE(5);

    private static final WebsocketEventType[] VALUES;

    static {
        WebsocketEventType[] values = values();
        VALUES = new WebsocketEventType[values.length];
        for (WebsocketEventType value : values) {
            VALUES[value.id] = value;
        }
    }


    private final int id;

    WebsocketEventType(int id) {
        this.id = id;
    }

    public static WebsocketEventType fromId(int id) {
        return VALUES.length > id ? VALUES[id] : null;
    }

    public int id() {
        return id;
    }
}
