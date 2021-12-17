/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateTagsPacket;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.Map;

/**
 * Manages information sent from the {@link ClientboundUpdateTagsPacket}. If that packet is not sent, all lists here
 * will remain empty, matching Java Edition behavior.
 */
public class TagCache {
    /* Blocks */
    private IntList leaves;
    private IntList wool;

    private IntList axeEffective;
    private IntList hoeEffective;
    private IntList pickaxeEffective;
    private IntList shovelEffective;

    private IntList requiresStoneTool;
    private IntList requiresIronTool;
    private IntList requiresDiamondTool;

    /* Items */
    private IntList flowers;
    private IntList foxFood;
    private IntList piglinLoved;

    public TagCache() {
        // Ensure all lists are non-null
        clear();
    }

    public void loadPacket(ClientboundUpdateTagsPacket packet) {
        Map<String, int[]> blockTags = packet.getTags().get("minecraft:block");
        this.leaves = IntList.of(blockTags.get("minecraft:leaves"));
        this.wool = IntList.of(blockTags.get("minecraft:wool"));

        this.axeEffective = IntList.of(blockTags.get("minecraft:mineable/axe"));
        this.hoeEffective = IntList.of(blockTags.get("minecraft:mineable/hoe"));
        this.pickaxeEffective = IntList.of(blockTags.get("minecraft:mineable/pickaxe"));
        this.shovelEffective = IntList.of(blockTags.get("minecraft:mineable/shovel"));

        this.requiresStoneTool = IntList.of(blockTags.get("minecraft:needs_stone_tool"));
        this.requiresIronTool = IntList.of(blockTags.get("minecraft:needs_iron_tool"));
        this.requiresDiamondTool = IntList.of(blockTags.get("minecraft:needs_diamond_tool"));

        Map<String, int[]> itemTags = packet.getTags().get("minecraft:item");
        this.flowers = IntList.of(itemTags.get("minecraft:flowers"));
        this.foxFood = IntList.of(itemTags.get("minecraft:fox_food"));
        this.piglinLoved = IntList.of(itemTags.get("minecraft:piglin_loved"));
    }

    public void clear() {
        this.leaves = IntLists.emptyList();
        this.wool = IntLists.emptyList();

        this.axeEffective = IntLists.emptyList();
        this.hoeEffective = IntLists.emptyList();
        this.pickaxeEffective = IntLists.emptyList();
        this.shovelEffective = IntLists.emptyList();

        this.requiresStoneTool = IntLists.emptyList();
        this.requiresIronTool = IntLists.emptyList();
        this.requiresDiamondTool = IntLists.emptyList();

        this.flowers = IntLists.emptyList();
        this.foxFood = IntLists.emptyList();
        this.piglinLoved = IntLists.emptyList();
    }

    public boolean isFlower(ItemMapping mapping) {
        return flowers.contains(mapping.getJavaId());
    }

    public boolean isFoxFood(ItemMapping mapping) {
        return foxFood.contains(mapping.getJavaId());
    }

    public boolean shouldPiglinAdmire(ItemMapping mapping) {
        return piglinLoved.contains(mapping.getJavaId());
    }

    public boolean isAxeEffective(BlockMapping blockMapping) {
        return axeEffective.contains(blockMapping.getJavaBlockId());
    }

    public boolean isHoeEffective(BlockMapping blockMapping) {
        return hoeEffective.contains(blockMapping.getJavaBlockId());
    }

    public boolean isPickaxeEffective(BlockMapping blockMapping) {
        return pickaxeEffective.contains(blockMapping.getJavaBlockId());
    }

    public boolean isShovelEffective(BlockMapping blockMapping) {
        return shovelEffective.contains(blockMapping.getJavaBlockId());
    }

    public boolean isShearsEffective(BlockMapping blockMapping) {
        int javaBlockId = blockMapping.getJavaBlockId();
        return leaves.contains(javaBlockId) || wool.contains(javaBlockId);
    }

    public boolean requiresStoneTool(BlockMapping blockMapping) {
        return requiresStoneTool.contains(blockMapping.getJavaBlockId());
    }

    public boolean requiresIronTool(BlockMapping blockMapping) {
        return requiresIronTool.contains(blockMapping.getJavaBlockId());
    }

    public boolean requiresDiamondTool(BlockMapping blockMapping) {
        return requiresDiamondTool.contains(blockMapping.getJavaBlockId());
    }
}
