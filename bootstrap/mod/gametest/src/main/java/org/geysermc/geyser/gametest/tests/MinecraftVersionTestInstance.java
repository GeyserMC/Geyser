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
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import org.geysermc.geyser.network.GameProtocol;

public class MinecraftVersionTestInstance extends GeyserTestInstance {
    public static final MapCodec<MinecraftVersionTestInstance> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        commonFields(instance)
            .and(Codec.STRING.fieldOf("version").forGetter(test -> test.version))
            .apply(instance, MinecraftVersionTestInstance::new)
    );
    private final String version;

    private MinecraftVersionTestInstance(RegistryOps<?> ops, boolean required, String version) {
        super(ops, required);
        this.version = version;
    }

    public MinecraftVersionTestInstance(HolderLookup.Provider registries, boolean required) {
        super(registries, required);
        this.version = SharedConstants.getCurrentVersion().id();
    }

    @Override
    public void run(GameTestHelper helper) {
        String current = SharedConstants.getCurrentVersion().id();
        helper.assertValueEqual(current, version, "running Minecraft version");
        helper.assertValueEqual(SharedConstants.getProtocolVersion(), GameProtocol.getJavaProtocolVersion(), "Java protocol version");
        helper.assertTrue(GameProtocol.getAllSupportedJavaVersions().contains(current), "GameProtocol must mark " + current + " as supported");
        helper.succeed();
    }

    @Override
    public MapCodec<MinecraftVersionTestInstance> codec() {
        return MAP_CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Geyser Minecraft Version Test for " + version);
    }
}
