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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponent;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryProvider;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.HashMap;

public class ResolvableComponentLoadingTestInstance extends GeyserTestInstance {
    public static final MapCodec<ResolvableComponentLoadingTestInstance> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        commonFields(instance)
            .and(Item.CODEC.fieldOf("item").forGetter(test -> test.item))
            .apply(instance, ResolvableComponentLoadingTestInstance::new)
    );
    private final Holder<Item> item;

    public ResolvableComponentLoadingTestInstance(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, boolean required, Holder<Item> item) {
        super(testEnvironments, required);
        this.item = item;
    }

    @Override
    public void run(GameTestHelper helper) {
        JavaRegistryProvider registries = createRegistryProvider(helper);
        var geyserItem = Registries.JAVA_ITEMS.get(BuiltInRegistries.ITEM.getId(item.value()));

        helper.assertFalse(geyserItem.resolvableComponents().isEmpty(), "a test was created for this item but it does not have any resolvable components loaded");

        DataComponents components = new DataComponents(new HashMap<>());
        for (ResolvableComponent<?> component : geyserItem.resolvableComponents()) {
            try {
                // Have to resolve into map to make sure casts and stuff are checked
                component.resolve(registries, components);
            } catch (Exception exception) {
                helper.fail("component " + component + " failed to resolve: " + exception);
            }
        }
        helper.succeed();
    }

    @Override
    public MapCodec<ResolvableComponentLoadingTestInstance> codec() {
        return MAP_CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Resolvable Component Loading for Item: " + item.unwrapKey().orElseThrow().identifier());
    }
}
