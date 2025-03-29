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

package org.geysermc.geyser.entity.type.living.animal;

/**
 * Extension to {@link VariantHolder} to make it easier to implement on mobs that use bedrock's metadata system to set their variants, which are quite common.
 *
 * @see VariantHolder
 */
public interface VariantIntHolder extends VariantHolder<VariantIntHolder.BuiltIn> {

    @Override
    default void setBedrockVariant(BuiltIn variant) {
        setBedrockVariantId(variant.ordinal());
    }

    /**
     * Should set the variant on bedrock's metadata. The bedrock ID has already been checked and is always valid.
     */
    void setBedrockVariantId(int bedrockId);

    /**
     * The enum constants should be ordered in the order of their bedrock network ID.
     *
     * @see org.geysermc.geyser.entity.type.living.animal.VariantHolder.BuiltIn
     */
    interface BuiltIn extends VariantHolder.BuiltIn {

        int ordinal();
    }
}
