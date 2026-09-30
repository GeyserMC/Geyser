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

package org.geysermc.geyser.inventory;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;

public class BrewingContainer extends Container {
    // On bedrock, it takes 400 ticks to brew anything. On Java, this can be customised, so we calculate the proper
    // fraction of 400 for custom brew times
    private static final double DEFAULT_TOTAL_BREW_TIME = 400.0;

    private double brewTime = 0;
    private double totalBrewTime = DEFAULT_TOTAL_BREW_TIME;

    public BrewingContainer(GeyserSession session, String title, int id, int size, @Nullable ContainerType containerType) {
        super(session, title, id, size, containerType);
    }

    public int updateBrewTime(int brewTime) {
        this.brewTime = brewTime;
        return calculateBedrockBrewTime();
    }

    public int updateTotalBrewTime(int totalBrewTime) {
        this.totalBrewTime = totalBrewTime;
        return calculateBedrockBrewTime();
    }

    private int calculateBedrockBrewTime() {
        if (totalBrewTime == DEFAULT_TOTAL_BREW_TIME) {
            return (int) brewTime;
        }
        return (int) Math.ceil(brewTime / totalBrewTime * DEFAULT_TOTAL_BREW_TIME);
    }
}
