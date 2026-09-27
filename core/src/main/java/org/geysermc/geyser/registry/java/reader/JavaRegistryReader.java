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

package org.geysermc.geyser.registry.java.reader;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.registry.java.RegistryUnit;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.Optional;

/**
 * A {@link JavaRegistryReader} is a function that takes a {@link Context}, holding an {@link Object} (the "registry entry", serialised as NBT),
 * and parses it into a {@link T}, possibly using other registries provided by {@link JavaRegistryProvider} or data from a {@link GeyserSession}.
 * All readers are registered, one for each of Java's networked-registries, in {@link JavaRegistryReaders}.
 *
 * <p>Note however that {@link GeyserSession} may not always be present when parsing registry data - it should generally only be used to pre-compute text component translations
 * (see {@link Context#parseDescription()}).</p>
 *
 * <p>If you don't care about the contents of the registry, and only about the network ID ≪-≫ key mapping, you should use the {@link JavaRegistryReader#UNIT} reader.</p>
 *
 * <p>Some readers, especially when trying to map dynamic Java content to static bedrock content, will need the {@link Key} of the entry they are parsing.
 * In this case, you should define a {@link KeyDependentJavaRegistryReader} instead. Please see the documentation there for the drawbacks of using one.</p>
 *
 * @param <T> the type this reader parses into
 * @see JavaRegistryReader#UNIT
 * @see Context
 * @see JavaRegistryReaders
 * @see KeyDependentJavaRegistryReader
 */
@FunctionalInterface
public interface JavaRegistryReader<T> extends KeyDependentJavaRegistryReader<T> {

    /**
     * A {@link JavaRegistryReader} that always returns {@link RegistryUnit#INSTANCE}.
     */
    JavaRegistryReader<RegistryUnit> UNIT = context -> RegistryUnit.INSTANCE;

    @Override
    default T read(RegistryEntryContext context) {
        return read((Context) context);
    }

    T read(Context context);

    /**
     * Creates a {@link Context} for parsing a registry entry. Should be used to parse inline entries.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param session the {@link GeyserSession}, may be empty
     * @param data the registry entry to parse
     * @return the created {@link Context}
     */
    static Context createContext(JavaRegistryProvider registries, Optional<GeyserSession> session, Object data) {
        return new Context() {
            @Override
            public Object data() {
                return data;
            }

            @Override
            public Optional<GeyserSession> session() {
                return session;
            }

            @Override
            public JavaRegistryProvider registries() {
                return registries;
            }
        };
    }

    /**
     * Provides context around a registry entry (serialised to NBT), such as a {@link JavaRegistryProvider}, and optionally the {@link GeyserSession}.
     */
    interface Context extends JavaRegistryProvider.Provider {

        /**
         * @return the registry entry to parse, serialised to NBT
         */
        Object data();

        /**
         * Note that this unsafely casts the data to an {@link NbtMap}, only use if you're sure the data is a compound tag.
         *
         * @return the registry entry, cast to an {@link NbtMap}
         */
        default NbtMap dataAsMap() {
            return (NbtMap) data();
        }

        /**
         * This may be an empty {@link Optional}, and you should generally depend on the presence of this {@link Optional} as little as possible.
         *
         * <p>For accessing registry data, use {@link JavaRegistryProvider.Provider#registries()} instead.</p>
         *
         * @return the current {@link GeyserSession}, may be empty
         */
        Optional<GeyserSession> session();

        /**
         * Attempts to use {@link Context#dataAsMap()} to get this registry entry as an {@link NbtMap}, and then tries to parse the {@code "description"} key
         * into a {@link Component}, using {@link MessageTranslator#componentFromNbtTag(Object)}. If the key did not exist, an empty component is returned.
         *
         * @return the {@code "description"} key from this entry, parsed as a {@link Component}
         */
        default Component parseDescription() {
             return MessageTranslator.componentFromNbtTag(dataAsMap().get("description"));
        }

        /**
         * Uses {@link MessageTranslator#convertFromNullableNbtTag(Optional, Object)} to convert the {@code "description"} key from
         * this registry entry into a bedrock text string. This will return {@code "MISSING GEYSER SESSION"} when {@link Context#session()} is empty.
         *
         * @return the {@code "description"} key from this entry, converted to a bedrock text string
         */
        default String deserializeDescription() {
            return MessageTranslator.convertFromNullableNbtTag(session(), dataAsMap().getOrDefault("description", null));
        }
    }
}
