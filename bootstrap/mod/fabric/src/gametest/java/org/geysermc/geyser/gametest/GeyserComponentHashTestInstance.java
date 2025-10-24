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

package org.geysermc.geyser.gametest;

import com.google.common.hash.HashCode;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.HashOps;
import org.geysermc.geyser.gametest.registries.GameTestJavaRegistryProvider;
import org.geysermc.geyser.item.hashing.DataComponentHashers;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GeyserComponentHashTestInstance extends GameTestInstance {

    private static final MapCodec<List<TypedDataComponent<?>>> TYPED_COMPONENT_CODEC = DataComponentType.PERSISTENT_CODEC
        .dispatchMap("component", list -> list.getFirst().type(), GeyserComponentHashTestInstance::typedComponentListCodec);
    public static final MapCodec<GeyserComponentHashTestInstance> CODEC = TYPED_COMPONENT_CODEC.xmap(GeyserComponentHashTestInstance::new,
        instance -> instance.testCases);

    private final List<TypedDataComponent<?>> testCases;

    public GeyserComponentHashTestInstance(List<TypedDataComponent<?>> testCases) {
        // TODO use default vanilla test environment
        super(new TestData<>(Holder.direct(new TestEnvironmentDefinition.AllOf(List.of())),
            ResourceLocation.withDefaultNamespace("empty"), 1, 1, true));
        this.testCases = testCases;
    }

    @Override
    public void run(@NotNull GameTestHelper helper) {
        RegistryOps<HashCode> ops = RegistryOps.create(HashOps.CRC32C_INSTANCE, helper.getLevel().registryAccess());

        for (TypedDataComponent<?> testCase : testCases) {
            // Encode vanilla component to buffer
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            TypedDataComponent.STREAM_CODEC.encode(buffer, testCase);

            // Read with MCPL
            int id = MinecraftTypes.readVarInt(buffer);
            DataComponent<?, ?> mcplComponent = DataComponentTypes.from(id).readDataComponent(buffer);

            // Hash both and compare
            int expected = testCase.encodeValue(ops).getOrThrow().asInt();
            GameTestJavaRegistryProvider registries = new GameTestJavaRegistryProvider(helper.getLevel().registryAccess());
            int geyser = DataComponentHashers.hash(registries, mcplComponent).asInt();

            helper.assertValueEqual(expected, geyser, Component.literal("Hash for component " + testCase));
        }

        // Succeed if nothing was thrown
        helper.succeed();
    }

    @Override
    public @NotNull MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull MutableComponent typeDescription() {
        // TODO more descriptive?
        return Component.literal("Geyser Data Component Hash Test");
    }

    // Generics are NOT friendly!!!
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static MapCodec<List<TypedDataComponent<?>>> typedComponentListCodec(DataComponentType component) {
        return ExtraCodecs.compactListCodec(component.codecOrThrow()).fieldOf("value")
            .xmap(
                values -> ((List<?>) values).stream()
                    .map(testCase -> new TypedDataComponent(component, testCase))
                    .map(testCase -> (TypedDataComponent<?>) testCase)
                    .toList(),
                typedComponents -> ((List<?>) typedComponents).stream()
                    .map(testCase -> ((TypedDataComponent<?>) testCase).value())
                    .toList());
    }
}
