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

package org.geysermc.geyser.translator.data;

import org.geysermc.event.Event;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.event.GeyserEventBus;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.TextTranslator;

import java.util.function.Function;

public interface CommonTranslationData extends JavaRegistryProvider.Provider, TextTranslator.LocaleBased {

    // TODO how many will use this? If not many then maybe just put it downstream
    PlatformType platformType();

    default <T extends Event> T fireEvent(T event) {
        return fireConnectionEvent($ -> event);
    }

    <T extends Event> T fireConnectionEvent(Function<GeyserConnection, T> constructor);

    record Impl(@Deprecated GeyserEventBus eventBus, @Deprecated GeyserConnection connection,
                PlatformType platformType, JavaRegistryProvider javaRegistries, String locale) implements CommonTranslationData {

        public Impl(GeyserSession session) {
            this(session.getGeyser().eventBus(), session, session.getGeyser().platformType(),
                session.javaRegistries(), session.locale());
        }

        @Override
        public <T extends Event> T fireConnectionEvent(Function<GeyserConnection, T> constructor) {
            T event = constructor.apply(connection);
            eventBus.fire(event);
            return event;
        }
    }

    @FunctionalInterface
    interface RecordBased extends CommonTranslationData {

        @Override
        default PlatformType platformType() {
            return commonData().platformType();
        }

        @Override
        default <T extends Event> T fireConnectionEvent(Function<GeyserConnection, T> constructor) {
            return commonData().fireConnectionEvent(constructor);
        }

        @Override
        default JavaRegistryProvider javaRegistries() {
            return commonData().javaRegistries();
        }

        @Override
        default String locale() {
            return commonData().locale();
        }

        CommonTranslationData commonData();
    }
}
