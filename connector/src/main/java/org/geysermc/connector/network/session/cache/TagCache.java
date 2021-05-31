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

package org.geysermc.connector.network.session.cache;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDeclareTagsPacket;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.registry.type.BlockMapping;

import java.util.Map;

/**
 * Manages information sent from the {@link ServerDeclareTagsPacket}. If that packet is not sent, all lists here
 * will remain empty, matching Java Edition behavior.
 */
public class TagCache {
    /* Blocks */
    private IntList wool = IntLists.emptyList();
    /* Items */
    private IntList flowers = IntLists.emptyList();
    private IntList piglinLoved = IntLists.emptyList();

    public void loadPacket(ServerDeclareTagsPacket packet) {
        Map<String, int[]> blockTags = packet.getBlockTags();
        this.wool = IntList.of(blockTags.get("minecraft:wool"));

        Map<String, int[]> itemTags = packet.getItemTags();
        this.flowers = IntList.of(itemTags.get("minecraft:flowers"));
        this.piglinLoved = IntList.of(itemTags.get("minecraft:piglin_loved"));
    }

    public void clear() {
        this.wool = IntLists.emptyList();

        this.flowers = IntLists.emptyList();
        this.piglinLoved = IntLists.emptyList();
    }

    public boolean isFlower(ItemEntry itemEntry) {
        return flowers.contains(itemEntry.getJavaId());
    }

    public boolean shouldPiglinAdmire(ItemEntry itemEntry) {
        return piglinLoved.contains(itemEntry.getJavaId());
    }

    public boolean isWool(BlockMapping blockMapping) {
        return wool.contains(blockMapping.getJavaBlockId());
    }
}
