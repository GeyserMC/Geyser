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
import net.minecraft.util.HashOps;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GeyserComponentHashTestInstance<T> extends GameTestInstance {

    private static final MapCodec<TypedDataComponent<?>> TYPED_COMPONENT_CODEC = DataComponentType.PERSISTENT_CODEC
        .dispatchMap("component", TypedDataComponent::type, GeyserComponentHashTestInstance::typedComponentCodec);
    public static final MapCodec<GeyserComponentHashTestInstance<?>> CODEC = TYPED_COMPONENT_CODEC.xmap(GeyserComponentHashTestInstance::new,
        instance -> instance.testValue);

    private final TypedDataComponent<T> testValue;

    public GeyserComponentHashTestInstance(TypedDataComponent<T> testValue) {
        // TODO use default vanilla test environment
        super(new TestData<>(Holder.direct(new TestEnvironmentDefinition.AllOf(List.of())),
            ResourceLocation.withDefaultNamespace("empty"), 1, 1, true));
        this.testValue = testValue;
    }

    @Override
    public void run(@NotNull GameTestHelper helper) {
        // Encode vanilla component to buffer
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        TypedDataComponent.STREAM_CODEC.encode(buffer, testValue);

        // Read with MCPL
        int id = MinecraftTypes.readVarInt(buffer);
        DataComponent<?, ?> mcplComponent = DataComponentTypes.from(id).readDataComponent(buffer);

        // Hash both and compare
        RegistryOps<HashCode> ops = RegistryOps.create(HashOps.CRC32C_INSTANCE, helper.getLevel().registryAccess());
        int expected = testValue.encodeValue(ops).getOrThrow().asInt();
        //int geyser = DataComponentHashers.hash(session, mcplComponent).asInt();
        int geyser = 0;

        helper.assertValueEqual(expected, geyser, Component.literal("Hash for component " + testValue));

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

    private static <T> MapCodec<TypedDataComponent<T>> typedComponentCodec(DataComponentType<T> component) {
        return component.codecOrThrow().fieldOf("value").xmap(value -> new TypedDataComponent<>(component, value), TypedDataComponent::value);
    }
}
