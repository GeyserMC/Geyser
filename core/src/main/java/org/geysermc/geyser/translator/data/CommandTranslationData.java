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

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.translator.command.BuiltInCommandEnums;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Accessors(fluent = true)
public final class CommandTranslationData implements CommonTranslationData.RecordBased {
    private static final Pattern LOWERCASE_LETTERS = Pattern.compile("^[a-z]+$");

    @Getter
    private final CommonTranslationData commonData;
    @Getter
    private final GeyserCommandSource commandSource;
    @Getter
    private final CommandRegistry commandRegistry;
    @Getter
    private final CommandNode[] nodes;
    @Getter
    private final int firstNodeIndex;

    private final Map<JavaRegistryKey<?>, String[]> registryElements = new Reference2ObjectOpenHashMap<>();
    private final Map<JavaRegistryKey<?>, String[]> registryElementsAndTags = new Reference2ObjectOpenHashMap<>();
    private final Map<BuiltInCommandEnums.Key, CommandEnumData> builtInEnums = new Reference2ObjectOpenHashMap<>();
    private final Map<Map<String, Set<CommandEnumConstraint>>, CommandEnumData> enums = new Object2ObjectOpenHashMap<>();
    private final Set<String> softEnumNames = new ObjectOpenHashSet<>();
    private final Set<String> hardEnumNames = new ObjectOpenHashSet<>();

    public CommandTranslationData(GeyserSession session, CommandNode[] nodes, int firstNodeIndex) {
        this.commonData = new CommonTranslationData.Impl(session);
        this.commandSource = session;
        this.commandRegistry = session.getGeyser().commandRegistry();
        this.nodes = nodes;
        this.firstNodeIndex = firstNodeIndex;

        BuiltInCommandEnums.register(new BuiltInCommandEnums.Registry() {
            @Override
            public void accept(BuiltInCommandEnums.Key key, String... values) {
                Map<String, Set<CommandEnumConstraint>> constraints = new Object2ObjectLinkedOpenHashMap<>();
                for (String value : values) {
                    constraints.put(value, Set.of());
                }
                // commandName doesn't matter in this context, all names should be unique anyway
                builtInEnums.put(key, constructEnum("", key.name(), constraints));
            }

            @Override
            public String[] teams() {
                return session.getWorldCache().getScoreboard().getTeamNames().toArray(String[]::new);
            }

            @Override
            public String[] levels() {
                return session.getLevels();
            }
        });
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
                .map(string -> '#' + string), Arrays.stream(getRegistryElements(registry)))
            .toArray(String[]::new));
    }

    public CommandEnumData builtInEnum(BuiltInCommandEnums.Key key) {
        return Objects.requireNonNull(builtInEnums.get(key), "built-in enum not registered: " + key.name());
    }

    public CommandEnumData constructEnum(String commandName, String proposedName, Map<String, Set<CommandEnumConstraint>> constraints) {
        return enums.computeIfAbsent(constraints, $ -> {
            boolean soft = needsSoftEnum(constraints);
            String safeName = findSafeEnumName(soft ? softEnumNames : hardEnumNames, commandName, proposedName);
            return new CommandEnumData(safeName, constraints, soft);
        });
    }

    private static String findSafeEnumName(Set<String> usedNames, String commandName, String proposedName) {
        if (usedNames.add(proposedName)) {
            return proposedName;
        }

        String safeName = commandName + "_" + proposedName;
        for (int i = 2; usedNames.contains(safeName); i++) {
            safeName = commandName + "_" + proposedName + "_" + i;
        }
        usedNames.add(safeName);
        return safeName;
    }

    /**
     * Bedrock rejects fixed enum values that are not lowercase ("Keywords in commands need to be
     * lower case"), while soft enum values keep their case. Brigadier literals are case-sensitive,
     * so the exact spelling has to survive the round trip. Vanilla BDS uses the same split.
     */
    private static boolean needsSoftEnum(Map<String, Set<CommandEnumConstraint>> constraints) {
        // TODO is this name or values specific
        for (String key : constraints.keySet()) {
            if (!LOWERCASE_LETTERS.matcher(key).matches()) {
                return true;
            }
        }
        return false;
    }
}
