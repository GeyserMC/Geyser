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

package org.geysermc.geyser.translator.command;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamData;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

final class BedrockCommandData {
    private static final Hash.Strategy<BedrockCommandInfo> PARAM_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(BedrockCommandInfo o) {
            int paramHash = Arrays.deepHashCode(o.paramData());
            if ("help".equals(o.name())) {
                paramHash = 31 * paramHash + 1;
            }
            return 31 * paramHash + o.description().hashCode();
        }

        @Override
        public boolean equals(BedrockCommandInfo a, BedrockCommandInfo b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            if ("help".equals(a.name()) && !"help".equals(b.name())) {
                // Merging this causes Bedrock to fallback to its own help command
                // Tested on Paper 1.20.4 with Essentials and Bedrock 1.21
                // https://github.com/GeyserMC/Geyser/issues/2573
                return false;
            }
            if (!a.description().equals(b.description())) return false;
            if (a.paramData().length != b.paramData().length) return false;
            for (int i = 0; i < a.paramData().length; i++) {
                CommandParamData[] a1 = a.paramData()[i].getOverloads();
                CommandParamData[] b1 = b.paramData()[i].getOverloads();
                if (a1.length != b1.length) return false;

                for (int j = 0; j < a1.length; j++) {
                    if (!a1[j].equals(b1[j])) return false;
                }
            }
            return true;
        }
    };

    final Map<BedrockCommandInfo, Set<String>> commands = new Object2ObjectOpenCustomHashMap<>(PARAM_STRATEGY);

    // These 2 are technically Java properties, but they're in this context as they're registered post-translation
    final Set<String> knownCommands = new ObjectOpenHashSet<>();
    final Set<String> restrictedCommands = new ObjectOpenHashSet<>();

    BedrockCommandData() {}
}
