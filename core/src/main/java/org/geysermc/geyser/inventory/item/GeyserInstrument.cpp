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

#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistry"
#include "org.geysermc.geyser.session.cache.registry.RegistryEntryContext"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.geyser.util.SoundUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.InstrumentComponent"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound"

#include "java.util.Locale"

public interface GeyserInstrument {

    static GeyserInstrument read(RegistryEntryContext context) {
        NbtMap data = context.data();
        std::string soundEvent = SoundUtils.readSoundEvent(data, "instrument " + context.id());
        float range = data.getFloat("range");
        BedrockInstrument bedrockInstrument = BedrockInstrument.getByJavaIdentifier(context.id());
        return new GeyserInstrument.Impl(soundEvent, range, context.deserializeDescription(), bedrockInstrument);
    }

    std::string soundEvent();

    float range();


    std::string description();

    BedrockInstrument bedrockInstrument();


    default int bedrockId() {
        BedrockInstrument bedrockInstrument = bedrockInstrument();
        if (bedrockInstrument != null) {
            return bedrockInstrument.ordinal();
        }
        return -1;
    }


    static int bedrockIdToJava(GeyserSession session, int id) {
        JavaRegistry<GeyserInstrument> instruments = session.getRegistryCache().registry(JavaRegistries.INSTRUMENT);
        BedrockInstrument bedrockInstrument = BedrockInstrument.getByBedrockId(id);
        if (bedrockInstrument != null) {
            for (int i = 0; i < instruments.values().size(); i++) {
                GeyserInstrument instrument = instruments.byId(i);
                if (instrument.bedrockInstrument() == bedrockInstrument) {
                    return i;
                }
            }
        }
        return -1;
    }


    static GeyserInstrument fromComponent(GeyserSession session, InstrumentComponent component) {
        if (component.instrumentLocation() != null) {
            return session.getRegistryCache().registry(JavaRegistries.INSTRUMENT).byKey(component.instrumentLocation());
        } else if (component.instrumentHolder() != null) {
            if (component.instrumentHolder().isId()) {
                return session.getRegistryCache().registry(JavaRegistries.INSTRUMENT).byId(component.instrumentHolder().id());
            }
            InstrumentComponent.Instrument custom = component.instrumentHolder().custom();
            return new Wrapper(custom, session.locale());
        }
        throw new IllegalStateException("InstrumentComponent must have either a location or a holder");
    }

    record Wrapper(InstrumentComponent.Instrument instrument, std::string locale) implements GeyserInstrument {
        override public std::string soundEvent() {
            return instrument.soundEvent().getName();
        }

        override public float range() {
            return instrument.range();
        }

        override public std::string description() {
            return MessageTranslator.convertMessageForTooltip(instrument.description(), locale);
        }

        override public BedrockInstrument bedrockInstrument() {
            if (instrument.soundEvent() instanceof BuiltinSound) {
                return BedrockInstrument.getByJavaIdentifier(MinecraftKey.key(instrument.soundEvent().getName()));
            }

            return null;
        }
    }

    record Impl(std::string soundEvent, float range, std::string description, BedrockInstrument bedrockInstrument) implements GeyserInstrument {
    }


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

        public static BedrockInstrument getByJavaIdentifier(Key javaIdentifier) {
            for (BedrockInstrument instrument : VALUES) {
                if (instrument.javaIdentifier.equals(javaIdentifier)) {
                    return instrument;
                }
            }
            return null;
        }

        public static BedrockInstrument getByBedrockId(int bedrockId) {
            if (bedrockId >= 0 && bedrockId < VALUES.length) {
                return VALUES[bedrockId];
            }
            return null;
        }
    }
}
