/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.recipe;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * A shaped crafting recipe that can be registered through the Geyser API.
 * <p>
 * Modded servers do not send the contents of crafting recipes to clients - the
 * {@code declare_recipes} packet only carries recipe ids, and the contents are only
 * sent for unlocked recipes. This lets server-side mods (e.g. Hydraulic) register the
 * full recipe set so Bedrock players get a populated recipe book.
 *
 * @param id            the Java recipe network id (the index in the synchronized recipe list)
 * @param width         the recipe grid width
 * @param height        the recipe grid height
 * @param ingredients   the ingredient grid, row-major; each entry is a Java item network id,
 *                      or {@code null} for an empty slot
 * @param result        the Java item network id of the result
 * @param resultCount   the result stack size
 */
public record CraftingRecipe(int id, int width, int height, List<@Nullable Integer> ingredients, int result, int resultCount) {
}
