/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.inventory.action;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public abstract class BaseAction implements Comparable<BaseAction>, Comparator<BaseAction> {
    // These handle tie breaking with equal priority actions to ensure they are executing FIFO
    final static AtomicInteger SEQUENCE = new AtomicInteger();
    final private int seq = SEQUENCE.getAndIncrement();

    @Setter
    protected Transaction transaction;

    public abstract void execute();

    public int getWeight() {
        return 0;
    }

    @Override
    public int compare(BaseAction left, BaseAction right) {
        int ret = left.getWeight() - right.getWeight();
        if (ret == 0) {
            return left.getSeq() - right.getSeq();
        }
        return ret;
    }

    @Override
    public int compareTo(BaseAction other) {
        int ret = getWeight() - other.getWeight();
        if (ret == 0) {
            return getSeq() - other.getSeq();
        }
        return ret;
    }
}
