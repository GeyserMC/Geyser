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

package org.geysermc.geyser.gametest;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;

import java.util.stream.Stream;

public class GameTestUtil {
    // Cursed codec to extract a RegistryOps
    public static final MapCodec<RegistryOps<?>> REGISTRY_OPS_MAP_CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<RegistryOps<?>> decode(DynamicOps<T> ops, MapLike<T> input) {
            if (ops instanceof RegistryOps<T> registryOps) {
                return DataResult.success(registryOps);
            }
            return DataResult.error(() -> "Registry ops required for parsing");
        }

        @Override
        public <T> RecordBuilder<T> encode(RegistryOps<?> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            // noop
            return prefix;
        }
    };

    public static TestData<Holder<TestEnvironmentDefinition<?>>> createEmptyTestData(RegistryOps<?> ops, boolean required) {
        return new TestData<>(ops.getter(Registries.TEST_ENVIRONMENT)
            .flatMap(getter -> getter.get(GameTestEnvironments.DEFAULT_KEY)).orElseThrow(),
            Identifier.withDefaultNamespace("empty"), 1, 1, required);
    }
}
