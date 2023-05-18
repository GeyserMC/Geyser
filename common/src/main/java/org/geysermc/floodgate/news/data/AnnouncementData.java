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

package org.geysermc.floodgate.news.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;

public final class AnnouncementData implements ItemData {
    private final Set<String> including = new HashSet<>();
    private final Set<String> excluding = new HashSet<>();

    private AnnouncementData() {}

    public static AnnouncementData read(JsonObject data) {
        AnnouncementData announcementData = new AnnouncementData();

        if (data.has("including")) {
            JsonArray including = data.getAsJsonArray("including");
            for (JsonElement element : including) {
                announcementData.including.add(element.getAsString());
            }
        }

        if (data.has("excluding")) {
            JsonArray including = data.getAsJsonArray("excluding");
            for (JsonElement element : including) {
                announcementData.excluding.add(element.getAsString());
            }
        }
        return announcementData;
    }

    public boolean isAffected(String project) {
        return !excluding.contains(project) && (including.isEmpty() || including.contains(project));
    }
}
