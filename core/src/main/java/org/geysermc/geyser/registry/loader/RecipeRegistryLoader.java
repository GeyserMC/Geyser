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
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.Ingredient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Populates the recipe registry with some recipes that Java does not send, to ensure they show up as intended
 * in the recipe book.
 */
public abstract class RecipeRegistryLoader implements RegistryLoader<String, Map<Object, List<GeyserRecipe>>> {

//    @Override
//    public Map<RecipeType, List<GeyserRecipe>> load(String input) {
//        if (true) {
//            return Collections.emptyMap();
//        }
//        Map<RecipeType, List<GeyserRecipe>> deserializedRecipes = new Object2ObjectOpenHashMap<>();
//
//        List<NbtMap> recipes;
//        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow("mappings/recipes.nbt")) {
//            try (NBTInputStream nbtStream = new NBTInputStream(new DataInputStream(stream))) {
//                recipes = ((NbtMap) nbtStream.readTag()).getList("recipes", NbtType.COMPOUND);
//            }
//        } catch (Exception e) {
//            throw new AssertionError(GeyserLocale.getLocaleStringLog("geyser.toolbox.fail.runtime_java"), e);
//        }
//
//        MinecraftCodecHelper helper = MinecraftCodec.CODEC.getHelperFactory().get();
//        for (NbtMap recipeCollection : recipes) {
//            var pair = getRecipes(recipeCollection, helper);
//            deserializedRecipes.put(pair.key(), pair.value());
//        }
//        return deserializedRecipes;
//    }

//    private static Pair<RecipeType, List<GeyserRecipe>> getRecipes(NbtMap recipes, MinecraftCodecHelper helper) {
//        List<NbtMap> typedRecipes = recipes.getList("recipes", NbtType.COMPOUND);
//        RecipeType recipeType = RecipeType.from(recipes.getInt("recipe_type", -1));
//        if (recipeType == RecipeType.CRAFTING_SPECIAL_TIPPEDARROW) {
//            return Pair.of(recipeType, getShapedRecipes(typedRecipes, helper));
//        } else {
//            return Pair.of(recipeType, getShapelessRecipes(typedRecipes, helper));
//        }
//    }

    private static List<GeyserRecipe> getShapelessRecipes(List<NbtMap> recipes, MinecraftCodecHelper helper) {
        List<GeyserRecipe> deserializedRecipes = new ObjectArrayList<>(recipes.size());
        for (NbtMap recipe : recipes) {
            ItemStack output = toItemStack(recipe.getCompound("output"), helper);
            List<NbtMap> rawInputs = recipe.getList("inputs", NbtType.COMPOUND);
            Ingredient[] javaInputs = new Ingredient[rawInputs.size()];
            for (int i = 0; i < rawInputs.size(); i++) {
                //javaInputs[i] = new Ingredient(new ItemStack[] {toItemStack(rawInputs.get(i), helper)});
            }
            //deserializedRecipes.add(new GeyserShapelessRecipe(javaInputs, output));
        }
        return deserializedRecipes;
    }

    private static List<GeyserRecipe> getShapedRecipes(List<NbtMap> recipes, MinecraftCodecHelper helper) {
        List<GeyserRecipe> deserializedRecipes = new ObjectArrayList<>(recipes.size());
        for (NbtMap recipe : recipes) {
            ItemStack output = toItemStack(recipe.getCompound("output"), helper);
            List<int[]> shape = recipe.getList("shape", NbtType.INT_ARRAY);

            // In the recipes mapping, each recipe is mapped by a number
            List<ItemStack> letterToRecipe = new ArrayList<>();
            for (NbtMap rawInput : recipe.getList("inputs", NbtType.COMPOUND)) {
                letterToRecipe.add(toItemStack(rawInput, helper));
            }

            Ingredient[] inputs = new Ingredient[shape.size() * shape.get(0).length];
            int i = 0;
            // Create a linear array of items from the "cube" of the shape
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

    /**
     * Converts our serialized NBT into an ItemStack.
     * id is the Java item ID as an integer, components is an optional String of the data components serialized
     * as bytes in Base64 (so MCProtocolLib can parse the data).
     */
    private static ItemStack toItemStack(NbtMap nbt, MinecraftCodecHelper helper) {
        int id = nbt.getInt("id");
        int count = nbt.getInt("count", 1);
        String componentsRaw = nbt.getString("components", null);
        if (componentsRaw != null) {
            byte[] bytes = Base64.getDecoder().decode(componentsRaw);
            ByteBuf buf = Unpooled.wrappedBuffer(bytes);
            DataComponents components = helper.readDataComponentPatch(buf);
            return new ItemStack(id, count, components);
        }
        return new ItemStack(id, count);
    }
}
