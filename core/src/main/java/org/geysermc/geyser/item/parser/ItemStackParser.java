/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.parser;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.item.DyeColor;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.SkullBlockEntityTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class ItemStackParser {
    private static final ItemStack AIR = new ItemStack(Items.AIR_ID);
    private static final Map<DataComponentType<?>, DataComponentParser<?, ?>> PARSERS = new Reference2ObjectOpenHashMap<>();

    private static <Raw, Parsed> void register(DataComponentType<Parsed> component, Class<Raw> rawClass, DataComponentParser<Raw, Parsed> parser) {
        if (PARSERS.containsKey(component)) {
            throw new IllegalStateException("Duplicate data component parser registered for " + component);
        }
        PARSERS.put(component, parser);
    }

    private static <Raw, Parsed> void registerSimple(DataComponentType<Parsed> component, Class<Raw> rawClass, Function<Raw, Parsed> parser) {
        register(component, rawClass, (session, raw) -> parser.apply(raw));
    }

    private static <Parsed> void registerSimple(DataComponentType<Parsed> component, Class<Parsed> parsedClass) {
        registerSimple(component, parsedClass, Function.identity());
    }

    private static <Parsed> void registerInstance(DataComponentType<Parsed> component, Parsed instance) {
        register(component, Object.class, (session, o) -> instance);
    }

    static {
        // TODO check this again
        // banner patterns []
        // base color [X]
        // charged projectiles [X]
        // custom model data []
        // dyed color [X]
        // enchantment glint override [X]
        // enchantments [X]
        // firework explosion []
        // item model [X]
        // map color [X]
        // pot decorations []
        // profile [X]
        registerSimple(DataComponentTypes.BASE_COLOR, String.class, raw -> DyeColor.getByJavaIdentifier(raw).ordinal());
        register(DataComponentTypes.CHARGED_PROJECTILES, List.class,
            (session, projectiles) -> projectiles.stream()
                .map(object -> parseItemStack(session, (NbtMap) object))
                .toList());

        registerSimple(DataComponentTypes.DYED_COLOR, Integer.class);
        registerSimple(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, Boolean.class);
        registerInstance(DataComponentTypes.ENCHANTMENTS, new ItemEnchantments(new Int2IntOpenHashMap()));
        registerSimple(DataComponentTypes.ITEM_MODEL, String.class, MinecraftKey::key);
        registerSimple(DataComponentTypes.MAP_COLOR, Integer.class);
        registerSimple(DataComponentTypes.PROFILE, NbtMap.class, SkullBlockEntityTranslator::parseResolvableProfile);

        register(DataComponentTypes.POTION_CONTENTS, NbtMap.class, (session, map) -> {
            // TODO
            return Optional.ofNullable(tag.getString("potion")).map(Potion::getByJavaIdentifier);
        });
    }

    private static <Raw, Parsed> void parseDataComponent(GeyserSession session, DataComponents patch, DataComponentType<?> type,
                                                         DataComponentParser<Raw, Parsed> parser, Object raw) {
        try {
            patch.put((DataComponentType<Parsed>) type, parser.parse(session, (Raw) raw));
        } catch (ClassCastException exception) {
            GeyserImpl.getInstance().getLogger().error("Received incorrect object type for component " + type + "!", exception);
        } catch (Exception exception) {
            GeyserImpl.getInstance().getLogger().error("Failed to parse component" + type + " from " + raw + "!", exception);
        }
    }

    public static @Nullable DataComponents parseDataComponentPatch(GeyserSession session, @Nullable NbtMap map) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        DataComponents patch = new DataComponents(new Reference2ObjectOpenHashMap<>());
        try {
            for (Map.Entry<String, Object> patchEntry : map.entrySet()) {
                String rawType = patchEntry.getKey();
                // When a component starts with a '!', indicates removal of the component from the default component set
                boolean removal = rawType.startsWith("!");
                if (removal) {
                    rawType = rawType.substring(1);
                }

                DataComponentType<?> type = DataComponentTypes.fromKey(MinecraftKey.key(rawType));
                if (type == null) {
                    GeyserImpl.getInstance().getLogger().warning("Received unknown data component " + rawType + " in NBT data component patch: " + map);
                } else if (removal) {
                    // Removals are easy, we don't have to parse anything
                    patch.put(type, null);
                } else {
                    DataComponentParser<?, ?> parser = PARSERS.get(type);
                    if (parser != null) {
                        parseDataComponent(session, patch, type, parser, patchEntry.getValue());
                    } else {
                        GeyserImpl.getInstance().getLogger().debug("Ignoring data component " + type + " whilst parsing NBT patch because there is no parser registered for it");
                    }
                }
            }
        } catch (Exception exception) {
            // TODO
        }
    }

    public static ItemStack parseItemStack(GeyserSession session, NbtMap map) {
        try {
            Item item = Registries.JAVA_ITEM_IDENTIFIERS.get(map.getString("id"));
            if (item == null) {
                GeyserImpl.getInstance().getLogger().warning("Unknown item " + map.getString("id") + " whilst trying to parse NBT item stack!");
                return AIR;
            }

            int id = item.javaId();
            int count = map.getInt("count");
            DataComponents patch = parseDataComponentPatch(session, map);
            return new ItemStack(id, count, patch);
        } catch (Exception exception) {
            // TODO
        }
    }

    @FunctionalInterface
    private interface DataComponentParser<Raw, Parsed> {

        Parsed parse(GeyserSession session, Raw raw) throws Exception;
    }
}
