/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators;

import com.github.steveice10.packetlib.packet.Packet;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Registry<T> {

    private final Map<Class<? extends T>, BiConsumer<? extends T, GeyserSession>> MAP = new HashMap<>();

    public static final Registry<Packet> JAVA = new Registry<>();

    public static <T extends Packet> void add(Class<T> clazz, BiConsumer<T, GeyserSession> translator) {
        JAVA.MAP.put(clazz, translator);
    }

    public <P extends T> void translate(Class<P> clazz, P p, GeyserSession s) {
        try {
            ((BiConsumer<P, GeyserSession>) JAVA.MAP.get(clazz)).accept(p, s);
        } catch (NullPointerException e) {
            GeyserLogger.DEFAULT.debug("could not translate packet " + p.getClass().getSimpleName());
        }
    }
}
