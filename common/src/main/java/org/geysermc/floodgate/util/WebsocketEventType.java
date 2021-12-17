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

package org.geysermc.floodgate.util;

public enum WebsocketEventType {
    /**
     * Sent once we successfully connected to the server
     */
    SUBSCRIBER_CREATED(0),
    /**
     * Sent every time a subscriber got added or disconnected
     */
    SUBSCRIBER_COUNT(1),
    /**
     * Sent once the creator disconnected. After this packet the server will automatically close the
     * connection once the queue size (sent in {@link #ADDED_TO_QUEUE} and {@link #SKIN_UPLOADED}
     * reaches 0.
     */
    CREATOR_DISCONNECTED(4),

    /**
     * Sent every time a skin got added to the upload queue
     */
    ADDED_TO_QUEUE(2),
    /**
     * Sent every time a skin got successfully uploaded
     */
    SKIN_UPLOADED(3),

    /**
     * Sent every time a news item was added
     */
    NEWS_ADDED(6),

    /**
     * Sent when the server wants you to know something. Currently used for violations that aren't
     * bad enough to close the connection
     */
    LOG_MESSAGE(5);

    private static final WebsocketEventType[] VALUES;

    static {
        WebsocketEventType[] values = values();
        VALUES = new WebsocketEventType[values.length];
        for (WebsocketEventType value : values) {
            VALUES[value.id] = value;
        }
    }

    /**
     * The ID is based of the time it got added. However, to keep the enum organized as time goes on,
     * it looks nicer to sort the events based of categories.
     */
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
