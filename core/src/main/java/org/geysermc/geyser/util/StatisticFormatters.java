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

package org.geysermc.geyser.util;

import org.geysermc.mcprotocollib.protocol.data.game.statistic.StatisticFormat;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

public final class StatisticFormatters {

    private static final Map<StatisticFormat, IntFunction<String>> FORMATTERS = new EnumMap<>(StatisticFormat.class);
    private static final DecimalFormat FORMAT = new DecimalFormat("###,###,##0.00");

    public static final IntFunction<String> INTEGER = NumberFormat.getIntegerInstance(Locale.US)::format;

    static {
        FORMATTERS.put(StatisticFormat.INTEGER, INTEGER);
        FORMATTERS.put(StatisticFormat.TENTHS, value -> FORMAT.format(value / 10d));
        FORMATTERS.put(StatisticFormat.DISTANCE, centimeter -> {
            double meter = centimeter / 100d;
            double kilometer = meter / 1000d;
            if (kilometer > 0.5) {
                return FORMAT.format(kilometer) + " km";
            } else if (meter > 0.5) {
                return FORMAT.format(meter) + " m";
            } else {
                return centimeter + " cm";
            }
        });
        FORMATTERS.put(StatisticFormat.TIME, ticks -> {
            double seconds = ticks / 20d;
            double minutes = seconds / 60d;
            double hours = minutes / 60d;
            double days = hours / 24d;
            double years = days / 365d;
            if (years > 0.5) {
                return FORMAT.format(years) + " y";
            } else if (days > 0.5) {
                return FORMAT.format(days) + " d";
            } else if (hours > 0.5) {
                return FORMAT.format(hours) + " h";
            } else if (minutes > 0.5) {
                return FORMAT.format(minutes) + " m";
            } else {
                return FORMAT.format(seconds) + " s";
            }
        });
    }

    public static IntFunction<String> get(StatisticFormat format) {
        return FORMATTERS.getOrDefault(format, INTEGER);
    }

    private StatisticFormatters() {
    }
}
