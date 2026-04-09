/*
 * Copyright (c) 2025-2026 GeyserMC. http://geysermc.org
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

import com.google.common.hash.HashCode;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.HashOps;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.gametest.GameTestUtil;
import org.geysermc.geyser.gametest.registries.GameTestJavaRegistryProvider;
import org.geysermc.geyser.item.hashing.DataComponentHashers;
import org.geysermc.geyser.item.hashing.MapHasher;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentHashTestInstance extends GameTestInstance {

    private static final MapCodec<List<TypedDataComponent<?>>> TYPED_COMPONENT_LIST_CODEC = DataComponentType.PERSISTENT_CODEC
        .dispatchMap("component", list -> list.getFirst().type(), ComponentHashTestInstance::typedComponentListCodec);
    private static final MapCodec<List<TypedDataComponent<?>>> MERGED_TYPED_COMPONENT_LIST_CODEC = TYPED_COMPONENT_LIST_CODEC.codec().listOf()
        .xmap(listOfLists -> listOfLists.stream().flatMap(List::stream).toList(), list -> {
            Map<DataComponentType<?>, List<TypedDataComponent<?>>> split = new HashMap<>();
            list.forEach(component -> {
                split.compute(component.type(), (type, typeList) -> {
                    if (typeList == null) {
                        typeList = new ArrayList<>();
                    }
                    typeList.add(component);
                    return typeList;
                });
            });
            return split.values().stream().toList();
        }).fieldOf("components");
    private static final MapCodec<List<TypedDataComponent<?>>> MERGED_AND_SINGLE_COMPONENT_MAP_CODEC = Codec.mapEither(MERGED_TYPED_COMPONENT_LIST_CODEC, TYPED_COMPONENT_LIST_CODEC)
        .xmap(Either::unwrap, Either::left);
    public static final MapCodec<ComponentHashTestInstance> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            GameTestUtil.REGISTRY_OPS_MAP_CODEC.forGetter(ignored -> null),
            MERGED_AND_SINGLE_COMPONENT_MAP_CODEC.forGetter(testInstance -> testInstance.testCases),
            Codec.BOOL.optionalFieldOf("required", true).forGetter(GameTestInstance::required)
        ).apply(instance, ComponentHashTestInstance::new)
    );

    private final List<TypedDataComponent<?>> testCases;

    public ComponentHashTestInstance(RegistryOps<?> ops, List<TypedDataComponent<?>> testCases, boolean required) {
        super(GameTestUtil.createEmptyTestData(ops, required));
        this.testCases = testCases;
    }

    public List<TypedDataComponent<?>> testCases() {
        return testCases;
    }

    @Override
    public void run(@NotNull GameTestHelper helper) {
        RegistryOps<Object> javaOps = helper.getLevel().registryAccess().createSerializationContext(JavaOps.INSTANCE);
        RegistryOps<HashCode> hashOps = helper.getLevel().registryAccess().createSerializationContext(HashOps.CRC32C_INSTANCE);

        for (TypedDataComponent<?> testCase : testCases) {
            // Encode vanilla component to buffer
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            TypedDataComponent.STREAM_CODEC.encode(buffer, testCase);

            // Read with MCPL
            int id = MinecraftTypes.readVarInt(buffer);
            DataComponent<?, ?> mcplComponent = DataComponentTypes.from(id).readDataComponent(buffer);

            Object encodedJavaValue = testCase.encodeValue(javaOps).getOrThrow();
            // Hash both and compare
            int expected = testCase.encodeValue(hashOps).getOrThrow().asInt();
            GameTestJavaRegistryProvider registries = new GameTestJavaRegistryProvider(helper.getLevel().registryAccess());
            int geyser = DataComponentHashers.hash(registries, mcplComponent).asInt();

            try {
                helper.assertValueEqual(expected, geyser, "hash for component " + encodedJavaValue);
            } catch (GameTestAssertException assertException) {
                GeyserImpl.getInstance().getLogger().info("Hash failed for component " + testCase.type() + " (" + testCase.value() + "), printing values of MapHasher");
                MapHasher.debug = true;
                DataComponentHashers.hash(registries, mcplComponent);
                MapHasher.debug = false;
                GeyserImpl.getInstance().getLogger().info("The Mojang encoded/expected value is printed in the exception message");
                throw assertException;
            }
        }

        // Succeed if nothing was thrown
        helper.succeed();
    }

    @Override
    public @NotNull MapCodec<ComponentHashTestInstance> codec() {
        return MAP_CODEC;
    }

    @Override
    protected @NotNull MutableComponent typeDescription() {
        // TODO more descriptive?
        return Component.literal("Geyser Data Component Hash Test");
    }

    // Generics are NOT friendly!!!
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static MapCodec<List<TypedDataComponent<?>>> typedComponentListCodec(DataComponentType component) {
        return ExtraCodecs.compactListCodec(component.codecOrThrow(), ExtraCodecs.nonEmptyList(component.codecOrThrow().listOf()))
            .fieldOf("value")
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
