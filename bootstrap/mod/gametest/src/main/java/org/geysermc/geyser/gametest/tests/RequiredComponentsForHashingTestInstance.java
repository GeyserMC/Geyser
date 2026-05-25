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
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import org.geysermc.geyser.gametest.GameTestUtil;
import org.geysermc.geyser.item.hashing.DataComponentHashers;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import java.util.HashSet;
import java.util.Set;

public class RequiredComponentsForHashingTestInstance extends GameTestInstance {
    public static final MapCodec<RequiredComponentsForHashingTestInstance> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            GameTestUtil.REGISTRY_OPS_MAP_CODEC.forGetter(ignored -> null),
            Codec.BOOL.optionalFieldOf("required", true).forGetter(GameTestInstance::required)
        ).apply(instance, RequiredComponentsForHashingTestInstance::new)
    );

    public RequiredComponentsForHashingTestInstance(RegistryOps<?> ops, boolean required) {
        super(GameTestUtil.createEmptyTestData(ops, required));
    }

    @Override
    public void run(GameTestHelper helper) {
        Set<DataComponentType<?>> componentsWithTests = new HashSet<>();
        for (GameTestInstance gameTest : helper.getLevel().registryAccess().lookupOrThrow(Registries.TEST_INSTANCE)) {
            if (gameTest instanceof ComponentHashTestInstance hashTestInstance) {
                hashTestInstance.testCases().stream()
                    .map(TypedDataComponent::type)
                    .forEach(componentsWithTests::add);
            }
        }

        for (DataComponentType<?> component : BuiltInRegistries.DATA_COMPONENT_TYPE) {
            org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType<?> mcpl = DataComponentTypes.from(BuiltInRegistries.DATA_COMPONENT_TYPE.getId(component));
            if (component.isTransient()) {
                helper.assertTrue(DataComponentHashers.NOT_HASHED.contains(mcpl), "transient component " + component + " must not be hashed");
            } else {
                helper.assertTrue(componentsWithTests.contains(component), "persistent component " + component + " must be hashed and tested");
            }
        }
        helper.succeed();
    }

    @Override
    public MapCodec<RequiredComponentsForHashingTestInstance> codec() {
        return MAP_CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Geyser Required Components For Hashing Test");
    }
}
