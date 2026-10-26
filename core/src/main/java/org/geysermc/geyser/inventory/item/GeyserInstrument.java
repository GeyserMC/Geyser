/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.inventory.item;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistry;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReader;
import org.geysermc.geyser.registry.java.reader.RegistryEntryContext;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.geyser.util.SoundUtils;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Instrument;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.Locale;

public interface GeyserInstrument {

    static GeyserInstrument read(JavaRegistryReader.Context context) {
        NbtMap data = context.dataAsMap();

        String parsingContext = "instrument";
        BedrockInstrument bedrockInstrument = null;
        if (context instanceof RegistryEntryContext keyedContext) {
            parsingContext += " " + keyedContext.id();
            bedrockInstrument = BedrockInstrument.getByJavaIdentifier(keyedContext.id());
        }

        String soundEvent = SoundUtils.readSoundEvent(data, parsingContext);
        float range = data.getFloat("range");
        return new GeyserInstrument.Impl(soundEvent, range, context.parseDescription(), bedrockInstrument);
    }

    String soundEvent();

    float range();

    Component description();

    @Nullable BedrockInstrument bedrockInstrument();

    /**
     * @return the ID of the Bedrock counterpart for this instrument. If there is none ({@link #bedrockInstrument()} is null), then -1 is returned.
     */
    default int bedrockId() {
        BedrockInstrument bedrockInstrument = bedrockInstrument();
        if (bedrockInstrument != null) {
            return bedrockInstrument.ordinal();
        }
        return -1;
    }

    /**
     * @return the ID of the Java counterpart for the given Bedrock ID. If an invalid Bedrock ID was given, or there is no counterpart, -1 is returned.
     */
    static int bedrockIdToJava(JavaRegistryProvider registries, int id) {
        JavaRegistry<GeyserInstrument> instruments = registries.registry(JavaRegistries.INSTRUMENT);
        BedrockInstrument bedrockInstrument = BedrockInstrument.getByBedrockId(id);
        if (bedrockInstrument != null) {
            for (int i = 0; i < instruments.size(); i++) {
                GeyserInstrument instrument = instruments.getOrThrow(i);
                if (instrument.bedrockInstrument() == bedrockInstrument) {
                    return i;
                }
            }
        }
        return -1;
    }

    // TODO test in 1.21.5
    static GeyserInstrument fromComponent(JavaRegistryProvider registries, Holder<Instrument> component) {
        if (component.isCustom()) {
            return new Wrapper(component.custom());
        } else if (component.isId()) {
            return JavaRegistries.INSTRUMENT.getOrThrow(registries, component.id());
        }
        throw new IllegalStateException("Instrument must either be custom or have an id");
    }

    record Wrapper(Instrument instrument) implements GeyserInstrument {
        @Override
        public String soundEvent() {
            return instrument.soundEvent().getName();
        }

        @Override
        public float range() {
            return instrument.range();
        }

        @Override
        public Component description() {
            return instrument.description();
        }

        @Override
        public BedrockInstrument bedrockInstrument() {
            if (instrument.soundEvent() instanceof BuiltinSound) {
                return BedrockInstrument.getByJavaIdentifier(MinecraftKey.key(instrument.soundEvent().getName()));
            }
            // Probably custom
            return null;
        }
    }

    record Impl(String soundEvent, float range, Component description, @Nullable BedrockInstrument bedrockInstrument) implements GeyserInstrument {
    }

    /**
     * Each vanilla instrument on Bedrock, ordered in their network IDs.
     */
    enum BedrockInstrument {
        PONDER,
        SING,
        SEEK,
        FEEL,
        ADMIRE,
        CALL,
        YEARN,
        DREAM;

        private static final BedrockInstrument[] VALUES = values();
        private final Key javaIdentifier;

        BedrockInstrument() {
            this.javaIdentifier = MinecraftKey.key(this.name().toLowerCase(Locale.ENGLISH) + "_goat_horn");
        }

        public static @Nullable BedrockInstrument getByJavaIdentifier(Key javaIdentifier) {
            for (BedrockInstrument instrument : VALUES) {
                if (instrument.javaIdentifier.equals(javaIdentifier)) {
                    return instrument;
                }
            }
            return null;
        }

        public static @Nullable BedrockInstrument getByBedrockId(int bedrockId) {
            if (bedrockId >= 0 && bedrockId < VALUES.length) {
                return VALUES[bedrockId];
            }
            return null;
        }
    }
}
