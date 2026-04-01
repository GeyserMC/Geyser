/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.loader;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.Ingredient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;


public abstract class RecipeRegistryLoader implements RegistryLoader<String, Map<Object, List<GeyserRecipe>>> {







//








//


















    private static List<GeyserRecipe> getShapelessRecipes(List<NbtMap> recipes) {
        List<GeyserRecipe> deserializedRecipes = new ObjectArrayList<>(recipes.size());
        for (NbtMap recipe : recipes) {
            ItemStack output = toItemStack(recipe.getCompound("output"));
            List<NbtMap> rawInputs = recipe.getList("inputs", NbtType.COMPOUND);
            Ingredient[] javaInputs = new Ingredient[rawInputs.size()];
            for (int i = 0; i < rawInputs.size(); i++) {
                //javaInputs[i] = new Ingredient(new ItemStack[] {toItemStack(rawInputs.get(i), helper)});
            }
            //deserializedRecipes.add(new GeyserShapelessRecipe(javaInputs, output));
        }
        return deserializedRecipes;
    }

    private static List<GeyserRecipe> getShapedRecipes(List<NbtMap> recipes) {
        List<GeyserRecipe> deserializedRecipes = new ObjectArrayList<>(recipes.size());
        for (NbtMap recipe : recipes) {
            ItemStack output = toItemStack(recipe.getCompound("output"));
            List<int[]> shape = recipe.getList("shape", NbtType.INT_ARRAY);

            
            List<ItemStack> letterToRecipe = new ArrayList<>();
            for (NbtMap rawInput : recipe.getList("inputs", NbtType.COMPOUND)) {
                letterToRecipe.add(toItemStack(rawInput));
            }

            Ingredient[] inputs = new Ingredient[shape.size() * shape.get(0).length];
            int i = 0;
            
            for (int j = 0; i < shape.size() * shape.get(0).length; j++) {
                for (int index : shape.get(j)) {
                    ItemStack stack = letterToRecipe.get(index);
                    //inputs[i++] = new Ingredient(new ItemStack[] {stack});
                }
            }
            //deserializedRecipes.add(new GeyserShapedRecipe(shape.size(), shape.get(0).length, inputs, output));
        }
        return deserializedRecipes;
    }

    
    private static ItemStack toItemStack(NbtMap nbt) {
        int id = nbt.getInt("id");
        int count = nbt.getInt("count", 1);
        String componentsRaw = nbt.getString("components", null);
        if (componentsRaw != null) {
            byte[] bytes = Base64.getDecoder().decode(componentsRaw);
            ByteBuf buf = Unpooled.wrappedBuffer(bytes);
            DataComponents components = MinecraftTypes.readDataComponentPatch(buf, false);
            return new ItemStack(id, count, components);
        }
        return new ItemStack(id, count);
    }
}
