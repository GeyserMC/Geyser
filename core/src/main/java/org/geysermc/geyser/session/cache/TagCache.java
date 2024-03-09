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

package org.geysermc.geyser.session.cache;

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.session.GeyserSession;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * Manages information sent from the {@link ClientboundUpdateTagsPacket}. If that packet is not sent, all lists here
 * will remain empty, matching Java Edition behavior.
 */
@ParametersAreNonnullByDefault
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
    private IntList axolotlTemptItems;
    private IntList creeperIgniters;
    private IntList fishes;
    private IntList flowers;
    private IntList foxFood;
    private IntList piglinLoved;
    private IntList smallFlowers;
    private IntList snifferFood;

    public TagCache() {
        // Ensure all lists are non-null
        clear();
    }

    public void loadPacket(GeyserSession session, ClientboundUpdateTagsPacket packet) {
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

        // Hack btw
        GeyserLogger logger = session.getGeyser().getLogger();
        int[] convertableToMud = blockTags.get("minecraft:convertable_to_mud");
        boolean emulatePost1_18Logic = convertableToMud != null && convertableToMud.length != 0;
        session.setEmulatePost1_18Logic(emulatePost1_18Logic);
        if (logger.isDebug()) {
            logger.debug("Emulating post 1.18 block predication logic for " + session.bedrockUsername() + "? " + emulatePost1_18Logic);
        }

        Map<String, int[]> itemTags = packet.getTags().get("minecraft:item");
        this.axolotlTemptItems = IntList.of(itemTags.get("minecraft:axolotl_tempt_items"));
        this.creeperIgniters = load(itemTags.get("minecraft:creeper_igniters"));
        this.fishes = IntList.of(itemTags.get("minecraft:fishes"));
        this.flowers = IntList.of(itemTags.get("minecraft:flowers"));
        this.foxFood = IntList.of(itemTags.get("minecraft:fox_food"));
        this.piglinLoved = IntList.of(itemTags.get("minecraft:piglin_loved"));
        this.smallFlowers = IntList.of(itemTags.get("minecraft:small_flowers"));
        this.snifferFood = load(itemTags.get("minecraft:sniffer_food"));

        // Hack btw
        boolean emulatePost1_13Logic = itemTags.get("minecraft:signs").length > 1;
        session.setEmulatePost1_13Logic(emulatePost1_13Logic);
        if (logger.isDebug()) {
            logger.debug("Emulating post 1.13 villager logic for " + session.bedrockUsername() + "? " + emulatePost1_13Logic);
        }
    }

    private IntList load(int @Nullable[] tags) {
        if (tags == null) {
            return IntLists.EMPTY_LIST;
        }
        return IntList.of(tags);
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

        this.axolotlTemptItems = IntLists.emptyList();
        this.creeperIgniters = IntLists.emptyList();
        this.fishes = IntLists.emptyList();
        this.flowers = IntLists.emptyList();
        this.foxFood = IntLists.emptyList();
        this.piglinLoved = IntLists.emptyList();
        this.smallFlowers = IntLists.emptyList();
        this.snifferFood = IntLists.emptyList();
    }

    public boolean isAxolotlTemptItem(Item item) {
        return axolotlTemptItems.contains(item.javaId());
    }

    public boolean isCreeperIgniter(Item item) {
        return creeperIgniters.contains(item.javaId());
    }

    public boolean isFish(GeyserItemStack itemStack) {
        return fishes.contains(itemStack.getJavaId());
    }

    public boolean isFlower(Item item) {
        return flowers.contains(item.javaId());
    }

    public boolean isFoxFood(Item item) {
        return foxFood.contains(item.javaId());
    }

    public boolean shouldPiglinAdmire(Item item) {
        return piglinLoved.contains(item.javaId());
    }

    public boolean isSmallFlower(GeyserItemStack itemStack) {
        return smallFlowers.contains(itemStack.getJavaId());
    }

    public boolean isSnifferFood(Item item) {
        return snifferFood.contains(item.javaId());
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
