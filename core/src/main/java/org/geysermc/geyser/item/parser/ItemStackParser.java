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

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.item.DyeColor;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.hashing.data.FireworkExplosionShape;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.translator.level.block.entity.SkullBlockEntityTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.BannerPatternLayer;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Fireworks;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PotionContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class to parse an item stack, or a data component patch, from NBT data.
 *
 * <p>This class does <em>NOT</em> parse all possible data components in a data component patch, only those that
 * can visually change the way an item looks. This class should/is usually used for parsing block entity NBT data,
 * such as for vault or shelf block entities.</p>
 *
 * <p>Be sure to update this class for Java updates!</p>
 */
// Lots of unchecked casting happens here. It should all be handled properly.
@SuppressWarnings("unchecked")
public final class ItemStackParser {
    private static final Map<DataComponentType<?>, DataComponentParser<?, ?>> PARSERS = new Reference2ObjectOpenHashMap<>();

    // We need the rawClass parameter here because the Raw type can't be inferred from the parser alone
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

    private static int javaItemIdentifierToNetworkId(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return Items.AIR_ID;
        }

        Item item = Registries.JAVA_ITEM_IDENTIFIERS.get(identifier);
        if (item == null) {
            GeyserImpl.getInstance().getLogger().warning("Received unknown item ID " + identifier + " whilst parsing NBT item stack!");
            return Items.AIR_ID;
        }
        return item.javaId();
    }

    private static ItemEnchantments parseEnchantments(GeyserSession session, NbtMap map) {
        Int2IntMap enchantments = new Int2IntOpenHashMap(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            enchantments.put(JavaRegistries.ENCHANTMENT.networkId(session, MinecraftKey.key(entry.getKey())), (int) entry.getValue());
        }
        return new ItemEnchantments(enchantments);
    }

    static {
        // The various ignored null-warnings are for things that should never be null as they shouldn't be missing from the data component
        // If they are null an exception will be thrown, but this will be caught with an error message logged

        register(DataComponentTypes.BANNER_PATTERNS, List.class, (session, raw) -> {
            List<NbtMap> casted = (List<NbtMap>) raw;
            List<BannerPatternLayer> layers = new ArrayList<>();
            for (NbtMap layer : casted) {
                DyeColor colour = DyeColor.getByJavaIdentifier(layer.getString("color"));

                // Patterns can be an ID or inline
                Object pattern = layer.get("pattern");
                Holder<BannerPatternLayer.BannerPattern> patternHolder;
                if (pattern instanceof String id) {
                    patternHolder = Holder.ofId(JavaRegistries.BANNER_PATTERN.networkId(session, MinecraftKey.key(id)));
                } else {
                    NbtMap inline = (NbtMap) pattern;
                    Key assetId = MinecraftKey.key(inline.getString("asset_id"));
                    String translationKey = inline.getString("translation_key");
                    patternHolder = Holder.ofCustom(new BannerPatternLayer.BannerPattern(assetId, translationKey));
                }
                layers.add(new BannerPatternLayer(patternHolder, colour.ordinal()));
            }
            return layers;
        });
        registerSimple(DataComponentTypes.BASE_COLOR, String.class, raw -> DyeColor.getByJavaIdentifier(raw).ordinal());
        register(DataComponentTypes.CHARGED_PROJECTILES, List.class,
            (session, projectiles) -> projectiles.stream()
                .map(object -> parseItemStack(session, (NbtMap) object))
                .toList());
        registerSimple(DataComponentTypes.CUSTOM_MODEL_DATA, NbtMap.class, raw -> {
            List<Float> floats = raw.getList("floats", NbtType.FLOAT);
            List<Boolean> flags = raw.getList("flags", NbtType.BYTE).stream().map(b -> b != 0).toList();
            List<String> strings = raw.getList("strings", NbtType.STRING);
            List<Integer> colours = raw.getList("colors", NbtType.INT);
            return new CustomModelData(floats, flags, strings, colours);
        });
        registerSimple(DataComponentTypes.DYED_COLOR, Integer.class);
        registerSimple(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, Boolean.class);
        register(DataComponentTypes.ENCHANTMENTS, NbtMap.class, ItemStackParser::parseEnchantments);
        registerSimple(DataComponentTypes.FIREWORK_EXPLOSION, NbtMap.class, raw -> {
            FireworkExplosionShape shape = FireworkExplosionShape.fromJavaIdentifier(raw.getString("shape"));
            List<Integer> colours = raw.getList("colors", NbtType.INT);
            List<Integer> fadeColours = raw.getList("fade_colors", NbtType.INT);
            boolean hasTrail = raw.getBoolean("has_trail");
            boolean hasTwinkle = raw.getBoolean("has_twinkle");
            return new Fireworks.FireworkExplosion(shape.ordinal(),
                colours.stream()
                    .mapToInt(i -> i) // We need to do this because MCPL wants an int[] array
                    .toArray(),
                fadeColours.stream()
                    .mapToInt(i -> i)
                    .toArray(),
                hasTrail, hasTwinkle);
        });
        registerSimple(DataComponentTypes.ITEM_MODEL, String.class, MinecraftKey::key);
        registerSimple(DataComponentTypes.MAP_COLOR, Integer.class);
        registerSimple(DataComponentTypes.POT_DECORATIONS, List.class, list -> list.stream()
            .map(item -> javaItemIdentifierToNetworkId((String) item))
            .toList());
        register(DataComponentTypes.POTION_CONTENTS, NbtMap.class, (session, map) -> {
            Potion potion = Potion.getByJavaIdentifier(map.getString("potion"));
            int customColour = map.getInt("custom_color", -1);
            // Not reading custom effects
            String customName = map.getString("custom_name", null);
            return new PotionContents(potion == null ? -1 : potion.ordinal(), customColour, List.of(), customName);
        });
        registerSimple(DataComponentTypes.PROFILE, NbtMap.class, SkullBlockEntityTranslator::parseResolvableProfile);
        register(DataComponentTypes.STORED_ENCHANTMENTS, NbtMap.class, ItemStackParser::parseEnchantments);
    }

    private static <Raw, Parsed> void parseDataComponent(GeyserSession session, DataComponents patch, DataComponentType<?> type,
                                                         DataComponentParser<Raw, Parsed> parser, Object raw) {
        try {
            patch.put((DataComponentType<Parsed>) type, parser.parse(session, (Raw) raw));
        } catch (ClassCastException exception) {
            GeyserImpl.getInstance().getLogger().debug("Received incorrect object type for component " + type + "!", exception);
        } catch (Exception exception) {
            GeyserImpl.getInstance().getLogger().debug("Failed to parse component" + type + " from " + raw + "!", exception);
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
            GeyserImpl.getInstance().getLogger().error("Failed to parse data component patch from NBT data!", exception);
        }

        return patch;
    }

    public static ItemStack parseItemStack(GeyserSession session, @Nullable NbtMap map) {
        if (map == null) {
            return new ItemStack(Items.AIR_ID);
        }

        try {
            int id = javaItemIdentifierToNetworkId(map.getString("id"));
            int count = map.getInt("count");
            DataComponents patch = parseDataComponentPatch(session, map.getCompound("components"));
            return new ItemStack(id, count, patch);
        } catch (Exception exception) {
            GeyserImpl.getInstance().getLogger().error("Failed to parse item stack from NBT data!", exception);
        }
        return new ItemStack(Items.AIR_ID);
    }

    /**
     * Shorthand method for calling the following methods:
     *
     * <ul>
     *     <li>{@link ItemStackParser#parseItemStack(GeyserSession, NbtMap)}</li>
     *     <li>{@link ItemTranslator#translateToBedrock(GeyserSession, ItemStack)}</li>
     *     <li>{@link BedrockItemBuilder#createItemNbt(ItemData)}</li>
     * </ul>
     */
    public static NbtMapBuilder javaItemStackToBedrock(GeyserSession session, @Nullable NbtMap map) {
        return BedrockItemBuilder.createItemNbt(ItemTranslator.translateToBedrock(session, parseItemStack(session, map)));
    }

    private ItemStackParser() {}

    @FunctionalInterface
    private interface DataComponentParser<Raw, Parsed> {

        Parsed parse(GeyserSession session, Raw raw) throws Exception;
    }
}
