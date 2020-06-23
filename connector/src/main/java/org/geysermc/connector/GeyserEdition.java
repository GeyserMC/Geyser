/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import lombok.Getter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class GeyserEdition {

    private static final Map<String, Class<? extends GeyserEdition>> EDITIONS = new HashMap<>();

    // -- Convenience Static Calls -- //
    public static GeyserEdition INSTANCE;

    // -- Variables -- //
    private final GeyserConnector connector;
    private final String edition;
    protected BedrockPacketCodec codec;
    protected String pongEdition;

    protected GeyserEdition(GeyserConnector connector, String edition) {
        INSTANCE = this;
        this.connector = connector;
        this.edition = edition;
    }

    public GeyserEdition(GeyserConnector connector) {
        throw new NotImplementedException();
    }

    public static void registerEdition(String edition, Class<? extends GeyserEdition> cls) {
        EDITIONS.put(edition, cls);
    }

    /**
     * Create a new GeyserEdition instance
     */
    public static GeyserEdition create(GeyserConnector connector, String edition) throws InvalidEditionException {
        if (!EDITIONS.containsKey(edition)) {
            throw new InvalidEditionException("Invalid Edition: " + edition);
        }

        try {
            return EDITIONS.get(edition).getConstructor(GeyserConnector.class).newInstance(connector);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new InvalidEditionException("Unable to create Edition: " + edition, e);
        }
    }

    public static class InvalidEditionException extends Exception {
        public InvalidEditionException(String message) {
            super(message);
        }

        public InvalidEditionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
