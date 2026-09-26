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

import net.kyori.adventure.text.Component;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.registry.java.RegistryUnit;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.Optional;

@FunctionalInterface
public interface JavaRegistryReader<T> extends KeyDependentJavaRegistryReader<T> {

    JavaRegistryReader<RegistryUnit> UNIT = context -> RegistryUnit.INSTANCE;

    @Override
    default T read(RegistryEntryContext context) {
        return read((Context) context);
    }

    T read(Context context);

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

    interface Context extends JavaRegistryProvider.Provider {

        Object data();

        Optional<GeyserSession> session();

         /**
         * Note that this unsafely casts the data to an {@link NbtMap}, only use if you're sure the data is a compound tag.
         */
         default NbtMap dataAsMap() {
            return (NbtMap) data();
        }

        default Component parseDescription() {
             return MessageTranslator.componentFromNbtTag(dataAsMap().get("description"));
        }

        default String deserializeDescription() {
            return MessageTranslator.convertFromNullableNbtTag(session(), dataAsMap().getOrDefault("description", null));
        }
    }
}
