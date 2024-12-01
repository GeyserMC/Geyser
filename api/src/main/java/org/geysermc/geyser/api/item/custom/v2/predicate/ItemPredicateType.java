/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.item.custom.v2.predicate;

import org.geysermc.geyser.api.item.custom.v2.predicate.data.ConditionPredicateData;
import org.geysermc.geyser.api.item.custom.v2.predicate.data.match.MatchPredicateData;

import java.util.HashMap;
import java.util.Map;

public class ItemPredicateType<T> {
    private static final Map<String, ItemPredicateType<?>> TYPES = new HashMap<>();

    public static final ItemPredicateType<ConditionPredicateData> CONDITION = create("condition");
    public static final ItemPredicateType<MatchPredicateData<?>> MATCH = create("match");

    public static ItemPredicateType<?> getType(String name) {
        return TYPES.get(name);
    }

    private static <T> ItemPredicateType<T> create(String name) {
        ItemPredicateType<T> type = new ItemPredicateType<>();
        TYPES.put(name, type);
        return type;
    }
}
