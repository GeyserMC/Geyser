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

package org.geysermc.geyser.registry.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.statistic.CustomStatistic;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.util.StatisticFormatters;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

public class StatisticsFormatsRegistryLoader implements RegistryLoader<String, Map<CustomStatistic, IntFunction<String>>> {

    @Override
    public Map<CustomStatistic, IntFunction<String>> load(String input) {
        InputStream statisticsStream = FileUtils.getResource(input);
        JsonNode statisticsNode;
        try {
            statisticsNode = GeyserImpl.JSON_MAPPER.readTree(statisticsStream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load statistics format data", e);
        }

        Map<CustomStatistic, IntFunction<String>> formats = new Object2ObjectOpenHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = statisticsNode.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            CustomStatistic statistic = CustomStatistic.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
            IntFunction<String> formatter = StatisticFormatters.get(entry.getValue().textValue());
            formats.put(statistic, formatter);
        }
        return formats;
    }
}
