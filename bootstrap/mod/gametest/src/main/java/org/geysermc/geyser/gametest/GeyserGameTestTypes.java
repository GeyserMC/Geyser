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

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.resources.Identifier;
import org.geysermc.geyser.gametest.tests.ComponentHashTestInstance;
import org.geysermc.geyser.gametest.tests.EntityMetadataTest;
import org.geysermc.geyser.gametest.tests.MinecraftVersionTestInstance;
import org.geysermc.geyser.gametest.tests.RequiredComponentsForHashingTestInstance;

public interface GeyserGameTestTypes {
    Identifier COMPONENT_HASH = createKey("component_hash");
    SingletonTestType REQUIRED_COMPONENTS_FOR_HASHING = createSingleton("required_components_for_hashing", RequiredComponentsForHashingTestInstance::new);
    Identifier ENTITY_METADATA = createKey("entity_metadata");
    SingletonTestType MINECRAFT_VERSION = createSingleton("minecraft_version", MinecraftVersionTestInstance::new);

    private static Identifier createKey(String name) {
        return Identifier.fromNamespaceAndPath("geyser", name);
    }

    private static SingletonTestType createSingleton(String name, SingletonTestType.Constructor constructor) {
        return new SingletonTestType(createKey(name), constructor);
    }

    private static void register(Identifier identifier, MapCodec<? extends GameTestInstance> codec) {
        Registry.register(BuiltInRegistries.TEST_INSTANCE_TYPE, identifier, codec);
    }

    private static void register(SingletonTestType type, MapCodec<? extends GameTestInstance> codec) {
        register(type.type, codec);
    }

    static void bootstrap() {
        register(COMPONENT_HASH, ComponentHashTestInstance.MAP_CODEC);
        register(REQUIRED_COMPONENTS_FOR_HASHING, RequiredComponentsForHashingTestInstance.MAP_CODEC);
        register(ENTITY_METADATA, EntityMetadataTest.MAP_CODEC);
        register(MINECRAFT_VERSION, MinecraftVersionTestInstance.MAP_CODEC);
    }

    record SingletonTestType(Identifier type, Constructor constructor) {

        @FunctionalInterface
        interface Constructor {

            GameTestInstance create(HolderLookup.Provider registries, boolean required);
        }
    }
}
