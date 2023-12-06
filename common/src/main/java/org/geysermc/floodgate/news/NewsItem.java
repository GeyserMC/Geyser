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

package org.geysermc.floodgate.news;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.floodgate.news.data.ItemData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class NewsItem {
    private final int id;
    private final boolean active;
    private final NewsType type;
    private final ItemData data;
    private final String message;
    private final Set<NewsItemAction> actions;
    private final String url;

    private NewsItem(
            int id, boolean active, NewsType type, ItemData data,
            String message, Set<NewsItemAction> actions, String url) {

        this.id = id;
        this.active = active;
        this.type = type;
        this.data = data;
        this.message = message;
        this.actions = Collections.unmodifiableSet(actions);
        this.url = url;
    }

    public static NewsItem readItem(JsonObject newsItem) {
        NewsType newsType = NewsType.getByName(newsItem.get("type").getAsString());
        if (newsType == null) {
            return null;
        }

        JsonObject messageObject = newsItem.getAsJsonObject("message");
        NewsItemMessage itemMessage = NewsItemMessage.getById(messageObject.get("id").getAsInt());

        String message = "Received an unknown news message type. Please update";
        if (itemMessage != null) {
            message = itemMessage.getFormattedMessage(messageObject.getAsJsonArray("args"));
        }

        Set<NewsItemAction> actions = new HashSet<>();
        for (JsonElement actionElement : newsItem.getAsJsonArray("actions")) {
            NewsItemAction action = NewsItemAction.getByName(actionElement.getAsString());
            if (action != null) {
                actions.add(action);
            }
        }

        return new NewsItem(
                newsItem.get("id").getAsInt(),
                newsItem.get("active").getAsBoolean(),
                newsType,
                newsType.read(newsItem.getAsJsonObject("data")),
                message,
                actions,
                newsItem.get("url").getAsString()
        );
    }

    public int getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public NewsType getType() {
        return type;
    }

    public ItemData getData() {
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T extends ItemData> T getDataAs(Class<T> type) {
        return (T) data;
    }

    @SuppressWarnings("unused")
    public String getRawMessage() {
        return message;
    }

    public String getMessage() {
        return message + " See " + getUrl() + " for more information.";
    }

    public Set<NewsItemAction> getActions() {
        return actions;
    }

    public String getUrl() {
        return url;
    }
}
