/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.custom.items;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.custom.CustomRenderOffsets;

import java.util.Set;

/**
 * This is used to store data for a custom item.
 */
public class FullyCustomItemData extends CustomItemData {
    private final String identifier;
    private final int javaId;

    private int stackSize = 64;

    private int maxDamage = 0;

    private String toolType = null;
    private String toolTier = null;

    private String armorType = null;
    private String armorTier = null;
    private int protectionValue = 0;

    private String translationString;

    private Set<String> repairMaterials;

    private boolean hasSuspiciousStewEffect = false;

    private Integer creativeCategory = null;
    private String creativeGroup = null;

    private boolean isHat = false;
    private boolean isTool = false;

    public FullyCustomItemData(@NonNull String name, @NonNull String identifier, int javaId) {
        super(name, new CustomItemRegistrationTypes());

        this.identifier = identifier;
        this.javaId = javaId;
    }

    /**
     * Gets the java identifier for this item.
     *
     * @return The java identifier for this item.
     */
    public String identifier() {
        return this.identifier;
    }

    /**
     * Gets the java item id of the item.
     *
     * @return the java item id of the item
     */
    public int javaId() {
        return this.javaId;
    }

    /**
     * Gets the stack size of the item.
     *
     * @return the stack size of the item
     */
    public int stackSize() {
        return this.stackSize;
    }

    /**
     * Sets the stack size of the item.
     *
     * @param stackSize the stack size of the item
     */
    public void stackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    /**
     * Gets the max damage of the item.
     *
     * @return the max damage of the item
     */
    public int maxDamage() {
        return this.maxDamage;
    }

    /**
     * Sets the max damage of the item.
     *
     * @param maxDamage the max damage of the item
     */
    public void maxDamage(int maxDamage) {
        this.maxDamage = maxDamage;
    }

    /**
     * Gets the tool type of the item.
     *
     * @return the tool type of the item
     */
    public @Nullable String toolType() {
        return this.toolType;
    }

    /**
     * Sets the tool type of the item.
     *
     * @param toolType the tool type of the item
     */
    public void toolType(@Nullable String toolType) {
        this.toolType = toolType;
    }

    /**
     * Gets the tool tier of the item.
     *
     * @return the tool tier of the item
     */
    public @Nullable String toolTier() {
        return this.toolTier;
    }

    /**
     * Sets the tool tier of the item.
     *
     * @param toolTier the tool tier of the item
     */
    public void toolTier(@Nullable String toolTier) {
        this.toolTier = toolTier;
    }

    /**
     * Gets the armor type of the item.
     *
     * @return the armor type of the item
     */
    public @Nullable String armorType() {
        return this.armorType;
    }

    /**
     * Sets the armor type of the item.
     *
     * @param armorType the armor type of the item
     */
    public void armorType(@Nullable String armorType) {
        this.armorType = armorType;
    }

    /**
     * Gets the armor tier of the item.
     *
     * @return the armor tier of the item
     */
    public @Nullable String armorTier() {
        return this.armorTier;
    }

    /**
     * Sets the armor tier of the item.
     *
     * @param armorTier the armor tier of the item
     */
    public void armorTier(@Nullable String armorTier) {
        this.armorTier = armorTier;
    }

    /**
     * Gets the armor protection value of the item.
     *
     * @return the armor protection value of the item
     */
    public int protectionValue() {
        return this.protectionValue;
    }

    /**
     * Sets the armor protection value of the item.
     *
     * @param protectionValue the armor protection value of the item
     */
    public void protectionValue(int protectionValue) {
        this.protectionValue = protectionValue;
    }

    /**
     * Gets the item's translation string.
     *
     * @return the item's translation string
     */
    public String translationString() {
        return this.translationString;
    }

    /**
     * Sets the item's translation string.
     *
     * @param translationString the item's translation string
     */
    public void translationString(String translationString) {
        this.translationString = translationString;
    }

    /**
     * Gets the repair materials of the item.
     *
     * @return the repair materials of the item
     */
    public @Nullable Set<String> repairMaterials() {
        return this.repairMaterials;
    }

    /**
     * Sets the repair materials of the item.
     *
     * @param repairMaterials the repair materials of the item
     */
    public void repairMaterials(@Nullable Set<String> repairMaterials) {
        this.repairMaterials = repairMaterials;
    }

    /**
     * Gets if the item has the suspicious stew effect.
     *
     * @return if the item has the suspicious stew effect
     */
    public boolean hasSuspiciousStewEffect() {
        return this.hasSuspiciousStewEffect;
    }

    /**
     * Sets if the item has the suspicious stew effect.
     *
     * @param suspiciousStewEffect if the item has the suspicious stew effect
     */
    public void hasSuspiciousStewEffect(boolean suspiciousStewEffect) {
        this.hasSuspiciousStewEffect = suspiciousStewEffect;
    }

    /**
     * Gets the item's creative category, or tab id.
     *
     * @return the item's creative category
     */
    public @Nullable Integer creativeCategory() {
        return this.creativeCategory;
    }

    /**
     * Sets the item's creative category, or tab id.
     *
     * @param creativeCategory the item's creative category
     */
    public void creativeCategory(@Nullable Integer creativeCategory) {
        this.creativeCategory = creativeCategory;
    }

    /**
     * Gets the item's creative group.
     *
     * @return the item's creative group
     */
    public @Nullable String creativeGroup() {
        return this.creativeGroup;
    }

    /**
     * Sets the item's creative group.
     *
     * @param creativeGroup the item's creative group
     */
    public void creativeGroup(@Nullable String creativeGroup) {
        this.creativeGroup = creativeGroup;
    }

    /**
     * Gets if the item is a hat. This is used to determine if the item should be rendered on the player's head, and
     * normally allow the player to equip it. This is not meant for armor.
     *
     * @return if the item is a hat
     */
    public boolean isHat() {
        return this.isHat;
    }

    /**
     * Sets if the item is a hat. This is used to determine if the item should be rendered on the player's head, and
     * normally allow the player to equip it. This is not meant for armor.
     *
     * @param isHat if the item is a hat
     */
    public void isHat(boolean isHat) {
        this.isHat = isHat;
    }

    /**
     * Gets if the item is a tool. This is used to set the render type of the item, if the item is handheld.
     *
     * @return if the item is a tool
     */
    public boolean isTool() {
        return this.isTool;
    }

    /**
     * Sets if the item is a tool. This is used to set the render type of the item, if the item is handheld.
     *
     * @param isTool if the item is a tool
     */
    public void isTool(boolean isTool) {
        this.isTool = isTool;
    }
}
