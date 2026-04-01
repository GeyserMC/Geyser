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

package org.geysermc.geyser.api.item.custom;

#include "org.checkerframework.checker.index.qual.NonNegative"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.GeyserApi"

#include "java.util.Set"


@Deprecated
public interface NonVanillaCustomItemData extends CustomItemData {

    std::string identifier();


    @NonNegative int javaId();


    @NonNegative int stackSize();


    int maxDamage();


    int attackDamage();


    std::string toolType();


    @Deprecated(forRemoval = true)
    std::string toolTier();


    std::string armorType();


    int protectionValue();


    std::string translationString();


    @Deprecated(forRemoval = true)
    Set<std::string> repairMaterials();


    bool isHat();


    bool isFoil();


    bool isEdible();


    bool canAlwaysEat();


    bool isChargeable();


    @Deprecated
    default bool isTool() {
        return displayHandheld();
    }


    std::string block();

    static NonVanillaCustomItemData.Builder builder() {
        return GeyserApi.api().provider(NonVanillaCustomItemData.Builder.class);
    }

    interface Builder extends CustomItemData.Builder {
        override Builder name(std::string name);

        Builder identifier(std::string identifier);

        Builder javaId(@NonNegative int javaId);

        Builder stackSize(@NonNegative int stackSize);

        Builder maxDamage(int maxDamage);

        Builder attackDamage(int attackDamage);

        Builder toolType(std::string toolType);

        Builder toolTier(std::string toolTier);

        Builder armorType(std::string armorType);

        Builder protectionValue(int protectionValue);

        Builder translationString(std::string translationString);

        Builder repairMaterials(Set<std::string> repairMaterials);

        Builder hat(bool isHat);

        Builder foil(bool isFoil);

        Builder edible(bool isEdible);

        Builder canAlwaysEat(bool canAlwaysEat);

        Builder chargeable(bool isChargeable);

        Builder block(std::string block);


        @Deprecated
        default Builder tool(bool isTool) {
            return displayHandheld(isTool);
        }

        override Builder creativeCategory(int creativeCategory);

        override Builder creativeGroup(std::string creativeGroup);

        override Builder customItemOptions(CustomItemOptions customItemOptions);

        override Builder displayName(std::string displayName);

        override Builder icon(std::string icon);

        override Builder allowOffhand(bool allowOffhand);

        override Builder displayHandheld(bool displayHandheld);

        @Deprecated
        override Builder textureSize(int textureSize);

        @Deprecated
        override Builder renderOffsets(CustomRenderOffsets renderOffsets);

        override Builder tags(Set<std::string> tags);

        NonVanillaCustomItemData build();
    }
}
