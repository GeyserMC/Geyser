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

package org.geysermc.geyser.session.cache;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.cloudburstmc.protocol.bedrock.packet.BiomeDefinitionListPacket;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.level.JavaBiome;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistry;
import org.geysermc.geyser.session.cache.registry.RegistryEntryData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks the custom biome definitions and numeric ids this session has sent to the client.
 *
 * <p>Definitions are registered globally, but which ones apply depends on the Java registry
 * the current backend sends. At the end of each configuration phase, {@link #reconcile()}
 * matches the global catalogue against the session's Java biome registry. Before the client
 * has spawned this only prepares a pending candidate; the candidate takes effect in
 * {@link #loginDefinitions()}, when its definitions are handed over for sending.
 * Once sent, a definition and its id keep their meaning for the whole session, even across
 * backend switches: the client may still have chunks loaded that reference them.</p>
 */
public final class CustomBiomeCache {
    // Where Bedrock custom biome ids start; the string-pool cap bounds the definition
    // count, keeping ids well below the wire format's signed-short maximum
    private static final int FIRST_CUSTOM_ID = 30000;
    // gophertunnel-based proxies reject definition lists and string pools over 1024 entries
    private static final int MAX_LIST_ENTRIES = 1024;
    // The vanilla definition that custom entries copy their wire-only values from
    private static final String REFERENCE_BIOME = "minecraft:plains";

    private final GeyserSession session;

    private final Map<Identifier, SentBiome> sent = new Object2ObjectOpenHashMap<>();
    private @Nullable Set<String> pooledStrings;
    private @Nullable PendingCatalogue pending;

    public CustomBiomeCache(GeyserSession session) {
        this.session = session;
    }

    /**
     * The biome definitions to send during login. When {@link #reconcile()} prepared a
     * pending candidate, returning it also commits it: the session's Java biome registry is
     * rewritten to the candidate's ids in the same call, so the mapping and the definition
     * send cannot be interleaved by chunk translation.
     */
    public BiomeDefinitions loginDefinitions() {
        if (pending == null) {
            return Registries.BIOMES.get();
        }
        return commitPending();
    }

    /**
     * Discards the pending login candidate. Called when the Java server starts a new
     * configuration phase: the candidate was built against the previous phase's registry,
     * and forms can spawn the client before the new phase provides a replacement.
     */
    public void discardPending() {
        pending = null;
    }

    /**
     * Matches the registered custom biomes against the session's current Java biome registry.
     * Called at the end of each configuration phase. Before the client has spawned, this
     * replaces the pending login candidate; afterwards (backend switches, and login paths
     * that spawn the client before the Java registry arrives) it resends the complete
     * definition list when new definitions were added.
     */
    public void reconcile() {
        // Nothing can have been sent when the catalogue is empty; it is frozen at startup
        Map<Identifier, CustomBiomeDefinition> catalogue = Registries.CUSTOM_BIOMES.get();
        if (catalogue.isEmpty()) {
            return;
        }

        JavaRegistry<JavaBiome> biomes = session.getRegistryCache().registry(JavaRegistries.BIOME);
        List<RegistryEntryData<JavaBiome>> entries = biomes.entries();

        List<Claim> claims = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            RegistryEntryData<JavaBiome> entry = entries.get(i);
            CustomBiomeDefinition definition = catalogue.get(Identifier.of(entry.key().asString()));
            if (definition != null) {
                claims.add(new Claim(i, definition, entry.data()));
            }
        }
        if (claims.isEmpty()) {
            return;
        }
        // Sorted by Bedrock identifier so id allocation doesn't depend on the registry's order
        claims.sort(Comparator.comparing(claim -> claim.definition().bedrockIdentifier().toString()));

        if (pooledStrings == null) {
            pooledStrings = new ObjectOpenHashSet<>();
            Registries.BIOMES.get().getDefinitions().forEach((name, data) -> {
                pooledStrings.add(name);
                List<String> tags = data.getTags();
                if (tags != null) {
                    pooledStrings.addAll(tags);
                }
            });
        }

        if (!session.isSentSpawnPacket()) {
            prepareLoginCandidate(claims, entries);
        } else {
            reconcileSpawned(claims, entries);
        }
    }

    /**
     * Builds the login candidate. Nothing is considered sent until {@link #commitPending()}
     * runs; a discarded candidate leaves no trace in the id space or the string pool. The
     * spawn packet precedes the commit, so {@link #sent} is always empty here and every
     * claim is new.
     */
    private void prepareLoginCandidate(List<Claim> claims, List<RegistryEntryData<JavaBiome>> entries) {
        Map<Identifier, SentBiome> union = new Object2ObjectOpenHashMap<>();
        Set<String> pool = new ObjectOpenHashSet<>(pooledStrings);
        Map<Key, Identifier> claimed = new Object2ObjectOpenHashMap<>();
        int skipped = 0;
        for (Claim claim : claims) {
            if (admit(claim, union, pool)) {
                claimed.put(entries.get(claim.index()).key(), claim.definition().bedrockIdentifier());
            } else {
                skipped++;
            }
        }
        pending = new PendingCatalogue(buildDefinitions(union), union, pool, claimed);
        warnSkipped(skipped);
    }

    private BiomeDefinitions commitPending() {
        PendingCatalogue pending = this.pending;
        this.pending = null;
        sent.putAll(pending.union());
        pooledStrings = pending.pool();

        List<RegistryEntryData<JavaBiome>> entries = session.getRegistryCache().registry(JavaRegistries.BIOME).entries();
        for (int i = 0; i < entries.size(); i++) {
            RegistryEntryData<JavaBiome> entry = entries.get(i);
            Identifier bedrockIdentifier = pending.claims().get(entry.key());
            if (bedrockIdentifier != null) {
                entries.set(i, new RegistryEntryData<>(entry.id(), entry.key(),
                    entry.data().withBedrockId(sent.get(bedrockIdentifier).id())));
            }
        }
        return pending.definitions();
    }

    private void reconcileSpawned(List<Claim> claims, List<RegistryEntryData<JavaBiome>> entries) {
        boolean unionGrew = false;
        for (Claim claim : claims) {
            if (!sent.containsKey(claim.definition().bedrockIdentifier())) {
                unionGrew |= admit(claim, sent, pooledStrings);
            }
        }

        if (unionGrew) {
            // Resend the complete list so definitions sent earlier keep their ids
            BiomeDefinitionListPacket packet = new BiomeDefinitionListPacket();
            packet.setBiomes(buildDefinitions(sent));
            session.sendUpstreamPacket(packet);
        }

        // Rewrite the registry only after the definition list is queued
        int skipped = 0;
        for (Claim claim : claims) {
            SentBiome sentBiome = sent.get(claim.definition().bedrockIdentifier());
            if (sentBiome == null || !climateMatches(sentBiome.data(), claim.biome())) {
                // Rejected at admission, or sent earlier with different climate; the Java
                // biome stays on the vanilla fallback path
                skipped++;
                continue;
            }
            RegistryEntryData<JavaBiome> entry = entries.get(claim.index());
            entries.set(claim.index(), new RegistryEntryData<>(entry.id(), entry.key(), entry.data().withBedrockId(sentBiome.id())));
        }
        warnSkipped(skipped);
    }

    private boolean admit(Claim claim, Map<Identifier, SentBiome> union, Set<String> pool) {
        CustomBiomeDefinition definition = claim.definition();
        // A malformed backend could send non-finite climate values; keep them off the wire
        if (!Float.isFinite(claim.biome().temperature()) || !Float.isFinite(claim.biome().downfall())) {
            return false;
        }

        Set<String> newStrings = new ObjectOpenHashSet<>();
        newStrings.add(definition.bedrockIdentifier().toString());
        newStrings.addAll(definition.tags());
        newStrings.removeAll(pool);

        // Every definition pools its unique name, so this also caps the definition count
        if (pool.size() + newStrings.size() > MAX_LIST_ENTRIES) {
            return false;
        }

        // Ids are never reused within a session, so the next slot after the union is always free
        int id = FIRST_CUSTOM_ID + union.size();
        pool.addAll(newStrings);
        union.put(definition.bedrockIdentifier(), new SentBiome(id, toData(id, claim)));
        return true;
    }

    /**
     * Whether the sent wire data still matches the Java biome's climate. A definition
     * keeps the climate it was first sent with for the whole session; when a later backend
     * has the same biome with different climate, its claim is rejected and the Java biome
     * falls back rather than rendering with stale values.
     */
    private static boolean climateMatches(BiomeDefinitionData data, JavaBiome biome) {
        return data.getTemperature() == biome.temperature() && data.getDownfall() == biome.downfall()
            && data.isRain() == biome.hasPrecipitation();
    }

    /**
     * Builds the wire definition from the Java biome the server sent, so climate always
     * matches the backend.
     */
    private static BiomeDefinitionData toData(int id, Claim claim) {
        JavaBiome biome = claim.biome();
        BiomeDefinitionData reference = Registries.BIOMES.get().getDefinitions().get(REFERENCE_BIOME);
        List<String> tags = claim.definition().tags().isEmpty() ? null : List.copyOf(claim.definition().tags());
        // Depth, scale, foliage snow and the map water color are wire fields without a Java
        // equivalent, so vanilla values are copied. chunkGenData describes client-side world
        // generation, which never happens for proxied chunks; keeping it null also sidesteps
        // the CloudburstMC v975+ nested write path, which cannot round-trip it
        return new BiomeDefinitionData(id, biome.temperature(), biome.downfall(), reference.getFoliageSnow(),
            reference.getDepth(), reference.getScale(), reference.getMapWaterColor(), biome.hasPrecipitation(), tags, null);
    }

    private static BiomeDefinitions buildDefinitions(Map<Identifier, SentBiome> union) {
        Map<String, BiomeDefinitionData> definitions = new LinkedHashMap<>(Registries.BIOMES.get().getDefinitions());
        union.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
            .forEach(entry -> definitions.put(entry.getKey().toString(), entry.getValue().data()));
        return new BiomeDefinitions(definitions);
    }

    private void warnSkipped(int skipped) {
        if (skipped > 0) {
            session.getGeyser().getLogger().warning(skipped + " custom biome mappings for " + session.bedrockUsername()
                + " could not be applied; affected Java biomes will use fallbacks");
        }
    }

    private record Claim(int index, CustomBiomeDefinition definition, JavaBiome biome) {
    }

    private record SentBiome(int id, BiomeDefinitionData data) {
    }

    private record PendingCatalogue(BiomeDefinitions definitions, Map<Identifier, SentBiome> union,
                                    Set<String> pool, Map<Key, Identifier> claims) {
    }
}
