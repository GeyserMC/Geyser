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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.cache.registry.JavaRegistry;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.MinecraftKey;

import java.util.Locale;

// TODO is this the right name?
public record GeyserInstrument(String soundEvent, float range, String description, @Nullable BedrockInstrument bedrockInstrument) {

    public static GeyserInstrument read(RegistryEntryContext context) {
        NbtMap data = context.data();
        // TODO this needs to turn into an util method as its duplicated
        Object soundEventObject = data.get("sound_event");
        String soundEvent;
        if (soundEventObject instanceof NbtMap map) {
            soundEvent = map.getString("sound_id");
        } else if (soundEventObject instanceof String string) {
            soundEvent = string;
        } else {
            soundEvent = "";
            GeyserImpl.getInstance().getLogger().debug("Sound event for " + context.id() + " was of an unexpected type! Expected string or NBT map, got " + soundEventObject);
        }

        float range = data.getFloat("range");
        String description = MessageTranslator.deserializeDescriptionForTooltip(context.session(), data);
        BedrockInstrument bedrockInstrument = BedrockInstrument.getByJavaIdentifier(context.id());
        return new GeyserInstrument(soundEvent, range, description, bedrockInstrument);
    }

    public int bedrockId() {
        if (bedrockInstrument != null) {
            return bedrockInstrument.ordinal();
        }
        return BedrockInstrument.VALUES.length;
    }

    // TODO can this be better?
    public static int bedrockIdToJava(JavaRegistry<GeyserInstrument> instruments, int id) {
        BedrockInstrument bedrockInstrument = BedrockInstrument.getByBedrockId(id);
        if (bedrockInstrument != null) {
            for (int i = 0; i < instruments.values().size(); i++) {
                GeyserInstrument instrument = instruments.byId(i);
                if (instrument.bedrockInstrument == bedrockInstrument) {
                    return i;
                }
            }
        }
        return id;
    }

    public enum BedrockInstrument {
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
