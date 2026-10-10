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

package org.geysermc.geyser.gametest.tests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.geysermc.geyser.gametest.util.CloudburstNbtOps;
import org.geysermc.geyser.gametest.util.GeyserGameTestsUtil;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;

import java.util.List;
import java.util.Optional;

public class GeyserHolderSetTestInstance extends GeyserTestInstance {
    public static final MapCodec<GeyserHolderSetTestInstance> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        commonFields(instance)
            .and(DynamicHolderSet.MAP_CODEC.forGetter(test -> test.set))
            .apply(instance, GeyserHolderSetTestInstance::new)
    );

    private final DynamicHolderSet<?> set;

    private GeyserHolderSetTestInstance(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, boolean required, DynamicHolderSet<?> set) {
        super(testEnvironments, required);
        this.set = set;
    }

    public <T> GeyserHolderSetTestInstance(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, boolean required,
                                           ResourceKey<? extends Registry<T>> registry, HolderSet<T> set) {
        this(testEnvironments, required, new DynamicHolderSet<>(registry, set));
    }

    @Override
    public void run(GameTestHelper helper) {
        RegistryAccess mojangRegistries = helper.getLevel().registryAccess();
        JavaRegistryProvider geyserRegistries = createRegistryProvider(helper);
        DynamicOps<Object> ops = mojangRegistries.createSerializationContext(CloudburstNbtOps.INSTANCE);

        JavaRegistryKey<?> geyserRegistry = GeyserGameTestsUtil.mojangKeyToGeyserKey(set.registry);
        GeyserHolderSet<?> geyserSet = GeyserHolderSet.readHolderSet(geyserRegistries, geyserRegistry, set.encode(ops).getOrThrow(), Optional.empty());

        // First, verify Geyser correctly identified tag holder sets
        // Named sets are tags
        if (set.set instanceof HolderSet.Named<?> namedSet) {
            helper.assertTrue(geyserSet.tag().isPresent(), "GeyserHolderSet must have a tag for named HolderSet: " + namedSet);
            helper.assertValueEqual(geyserSet.tag().map(Tag::tag).orElseThrow(),
                GeyserGameTestsUtil.identifierToKey(namedSet.key().location()), "tag of named HolderSet");
        } else {
            helper.assertTrue(geyserSet.tag().isEmpty(), "GeyserHolderSet must not have a tag for direct HolderSet");
        }

        // Then, resolve the holders, and verify they match Mojang
        assertResolvedSetIsSame(helper, mojangRegistries, geyserSet.resolveHolders(geyserRegistries));
        helper.succeed();
    }

    private <Geyser> void assertResolvedSetIsSame(GameTestHelper helper, RegistryAccess registries, List<Holder<Geyser>> geyserHolders) {
        helper.assertValueEqual(geyserHolders.size(), set.set.size(), "size of HolderSet");

        for (int i = 0; i < geyserHolders.size(); i++) {
            Holder<Geyser> geyserHolder = geyserHolders.get(i);
            boolean mojangIsReference = set.set.get(i).unwrapKey().isPresent();
            if (geyserHolder.isId()) {
                helper.assertTrue(mojangIsReference, "Mojang Holder must be a reference if Geyser holder has an ID");
                helper.assertValueEqual(geyserHolder.id(), set.id(registries, i), "network ID of holder " + i);
            } else {
                // We can't match exact values here since they can be completely different, so just confirm they're both custom holders
                helper.assertFalse(mojangIsReference, "Mojang Holder must not be a reference if Geyser holder is custom");
            }
        }
    }

    @Override
    public MapCodec<GeyserHolderSetTestInstance> codec() {
        return MAP_CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Geyser Holder Set Test for registry " + set.registry.identifier());
    }

    private record DynamicHolderSet<T>(ResourceKey<? extends Registry<T>> registry, HolderSet<T> set) {
        private static final Codec<ResourceKey<? extends Registry<?>>> REGISTRY_KEY_CODEC = Identifier.CODEC.xmap(ResourceKey::createRegistryKey, ResourceKey::identifier);
        private static final MapCodec<DynamicHolderSet<?>> MAP_CODEC = REGISTRY_KEY_CODEC
            .dispatchMap("registry", set -> set.registry, DynamicHolderSet::codec);

        public int id(RegistryAccess registries, int index) {
            return registries.lookupOrThrow(registry).getIdOrThrow(set.get(index).value());
        }

        public <E> DataResult<E> encode(DynamicOps<E> ops) {
            return holderSetCodec(registry).encodeStart(ops, set);
        }

        @SuppressWarnings("unchecked")
        private static <T> MapCodec<DynamicHolderSet<T>> codec(ResourceKey<? extends Registry<?>> uncasted) {
            ResourceKey<? extends Registry<T>> registry = (ResourceKey<? extends Registry<T>>) uncasted;
            return holderSetCodec(registry).fieldOf("set")
                .xmap(set -> new DynamicHolderSet<>(registry, set), DynamicHolderSet::set);
        }

        private static <T> Codec<HolderSet<T>> holderSetCodec(ResourceKey<? extends Registry<T>> registry) {
            return GeyserGameTestsUtil.getSyncedRegistryData(registry)
                .map(data -> RegistryCodecs.holderSet(registry, data.elementCodec()))
                .orElseGet(() -> RegistryCodecs.holderSet(registry));
        }
    }
}
