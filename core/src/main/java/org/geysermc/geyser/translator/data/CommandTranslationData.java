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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

@Accessors(fluent = true)
@Getter
public final class CommandTranslationData implements CommonTranslationData.RecordBased {
    private final CommonTranslationData commonData;
    private final GeyserCommandSource commandSource;
    private final CommandRegistry commandRegistry;
    private final CommandNode[] nodes;
    private final String[] levels;
    private final CommandEnumData teams;
    private final int firstNodeIndex;

    private final Map<JavaRegistryKey<?>, String[]> registryElements = new Reference2ObjectOpenHashMap<>();
    private final Map<JavaRegistryKey<?>, String[]> registryElementsAndTags = new Reference2ObjectOpenHashMap<>();
    private final Map<String, String> softEnumNames = new Object2ObjectOpenHashMap<>();

    public CommandTranslationData(GeyserSession session, CommandNode[] nodes, int firstNodeIndex) {
        this.commonData = new CommonTranslationData.Impl(session);
        this.commandSource = session;
        this.commandRegistry = session.getGeyser().commandRegistry();
        this.nodes = nodes;
        this.levels = session.getLevels();
        this.teams = new CommandEnumData("Geyser_Teams", session.getWorldCache().getScoreboard().getTeamNames(), true);
        this.firstNodeIndex = firstNodeIndex;
    }

    public String[] getRegistryElements(JavaRegistryKey<?> registry) {
        return registryElements.computeIfAbsent(registry, $ -> javaRegistries().registry(registry).keys().stream()
            .map(Key::asString)
            .toArray(String[]::new));
    }

    public String[] getRegistryElementsAndTags(JavaRegistryKey<?> registry) {
        return registryElementsAndTags.computeIfAbsent(registry, $ -> Stream.concat(javaRegistries().tags(registry)
                .map(Tag::tag)
                .map(Key::asString)
                // Prefix tags with a '#'
                .map(string -> "#" + string), Arrays.stream(getRegistryElements(registry)))
            .toArray(String[]::new));
    }
}
