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

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import org.geysermc.geyser.gametest.registries.GameTestJavaRegistryProvider;
import org.geysermc.geyser.session.cache.registry.JavaRegistryProvider;

public abstract class GeyserTestInstance extends GameTestInstance {

    protected GeyserTestInstance(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, boolean required) {
        super(createEmptyTestData(testEnvironments, required));
    }

    protected JavaRegistryProvider createRegistryProvider(GameTestHelper helper) {
        return new GameTestJavaRegistryProvider(helper.getLevel().registryAccess());
    }

    protected static <T extends GeyserTestInstance> Products.P2<RecordCodecBuilder.Mu<T>, HolderGetter<TestEnvironmentDefinition<?>>, Boolean> commonFields(RecordCodecBuilder.Instance<T> instance) {
        return instance.group(
            RegistryOps.retrieveGetter(Registries.TEST_ENVIRONMENT),
            Codec.BOOL.optionalFieldOf("required", true).forGetter(GameTestInstance::required)
        );
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> createEmptyTestData(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, boolean required) {
        return new TestData<>(testEnvironments.getOrThrow(GameTestEnvironments.DEFAULT_KEY),
            Identifier.withDefaultNamespace("empty"), 1, 1, required);
    }
}
