/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.cloudburstmc.protocol.bedrock.data.TrimMaterial;
import org.cloudburstmc.protocol.bedrock.data.TrimPattern;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.living.animal.tameable.WolfEntity;
import org.geysermc.geyser.inventory.item.BannerPattern;
import org.geysermc.geyser.inventory.recipe.TrimRecipe;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistry;
import org.geysermc.geyser.session.cache.registry.SimpleJavaRegistry;
import org.geysermc.geyser.text.TextDecoration;
import org.geysermc.geyser.translator.level.BiomeTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Stores any information sent via Java registries. May not contain all data in a given registry - we'll strip what's
 * unneeded.
 *
 * Crafted as of 1.20.5 for easy "add new registry" functionality in the future.
 */
@Accessors(fluent = true)
@Getter
public final class RegistryCache {
    private static final Map<String, BiConsumer<RegistryCache, List<RegistryEntry>>> REGISTRIES = new HashMap<>();

    static {
        register("chat_type", cache -> cache.chatTypes, ($, entry) -> TextDecoration.readChatType(entry));
        register("dimension_type", cache -> cache.dimensions, ($, entry) -> JavaDimension.read(entry));
        register("trim_material", cache -> cache.trimMaterials, TrimRecipe::readTrimMaterial);
        register("trim_pattern", cache -> cache.trimPatterns, TrimRecipe::readTrimPattern);
        register("worldgen/biome", (cache, array) -> cache.biomeTranslations = array, BiomeTranslator::loadServerBiome);
        register("banner_pattern", cache -> cache.bannerPatterns, ($, entry) -> BannerPattern.getByJavaIdentifier(entry.getId()));
        register("wolf_variant", cache -> cache.wolfVariants, ($, entry) -> WolfEntity.WolfVariant.getByJavaIdentifier(entry.getId()));
    }

    @Getter(AccessLevel.NONE)
    private final GeyserSession session;

    /**
     * Java -> Bedrock biome network IDs.
     */
    private int[] biomeTranslations;
    private final JavaRegistry<TextDecoration> chatTypes = new SimpleJavaRegistry<>();
    /**
     * All dimensions that the client could possibly connect to.
     */
    private final JavaRegistry<JavaDimension> dimensions = new SimpleJavaRegistry<>();
    private final JavaRegistry<TrimMaterial> trimMaterials = new SimpleJavaRegistry<>();
    private final JavaRegistry<TrimPattern> trimPatterns = new SimpleJavaRegistry<>();

    private final JavaRegistry<BannerPattern> bannerPatterns = new SimpleJavaRegistry<>();
    private final JavaRegistry<WolfEntity.WolfVariant> wolfVariants = new SimpleJavaRegistry<>();

    public RegistryCache(GeyserSession session) {
        this.session = session;
    }

    /**
     * Loads a registry in, if we are tracking it.
     */
    public void load(ClientboundRegistryDataPacket packet) {
        var reader = REGISTRIES.get(packet.getRegistry());
        if (reader != null) {
            reader.accept(this, packet.getEntries());
        } else {
            GeyserImpl.getInstance().getLogger().debug("Ignoring registry of type " + packet.getRegistry());
        }
    }

    /**
     * @param registry the Java registry resource location, without the "minecraft:" prefix.
     * @param localCacheFunction which local field in RegistryCache are we caching entries for this registry?
     * @param reader converts the RegistryEntry NBT into a class file
     * @param <T> the class that represents these entries.
     */
    private static <T> void register(String registry, Function<RegistryCache, JavaRegistry<T>> localCacheFunction, BiFunction<GeyserSession, RegistryEntry, T> reader) {
        REGISTRIES.put("minecraft:" + registry, (registryCache, entries) -> {
            JavaRegistry<T> localCache = localCacheFunction.apply(registryCache);
            // Clear each local cache every time a new registry entry is given to us
            // (e.g. proxy server switches)
            List<T> builder = new ArrayList<>(entries.size());
            for (int i = 0; i < entries.size(); i++) {
                RegistryEntry entry = entries.get(i);
                // This is what Geyser wants to keep as a value for this registry.
                T cacheEntry = reader.apply(registryCache.session, entry);
                builder.add(i, cacheEntry);
            }
            localCache.reset(builder);
        });
    }

    /**
     * @param localCacheFunction the int array to set the final values to.
     */
    private static void register(String registry, BiConsumer<RegistryCache, int[]> localCacheFunction, ToIntFunction<RegistryEntry> reader) {
        REGISTRIES.put("minecraft:" + registry, (registryCache, entries) -> {
            Int2IntMap temp = new Int2IntOpenHashMap();
            int greatestId = 0;
            for (int i = 0; i < entries.size(); i++) {
                RegistryEntry entry = entries.get(i);
                // This is what Geyser wants to keep as a value for this registry.
                int cacheEntry = reader.applyAsInt(entry);
                temp.put(i, cacheEntry);
                if (i > greatestId) {
                    // Maximum registry ID, so far. Make sure the final array is at least this large.
                    greatestId = i;
                }
            }

            int[] array = new int[greatestId + 1];
            for (Int2IntMap.Entry entry : temp.int2IntEntrySet()) {
                array[entry.getIntKey()] = entry.getIntValue();
            }
            localCacheFunction.accept(registryCache, array);
        });
    }
}
