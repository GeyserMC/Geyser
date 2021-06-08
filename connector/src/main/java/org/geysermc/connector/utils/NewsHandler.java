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

package org.geysermc.connector.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserLogger;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.floodgate.news.NewsItem;
import org.geysermc.floodgate.news.NewsItemAction;
import org.geysermc.floodgate.news.data.BuildSpecificData;
import org.geysermc.floodgate.news.data.CheckAfterData;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NewsHandler {
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final GeyserLogger logger = GeyserConnector.getInstance().getLogger();
    private final Gson gson = new Gson();

    private final Map<Integer, NewsItem> activeNewsItems = new HashMap<>();
    private final String branch;
    private final int build;

    private boolean firstCheck = true;

    public NewsHandler(String branch, int build) {
        this.branch = branch;
        this.build = build;

        executorService.scheduleWithFixedDelay(this::checkNews, 0, 30, TimeUnit.MINUTES);
    }

    private void schedule(long delayMs) {
        executorService.schedule(this::checkNews, delayMs, TimeUnit.MILLISECONDS);
    }

    private void checkNews() {
        try {
            String body = WebUtils.getBody(Constants.NEWS_OVERVIEW_URL);
            JsonArray array = gson.fromJson(body, JsonArray.class);

            try {
                for (JsonElement newsItemElement : array) {
                    NewsItem newsItem = NewsItem.readItem(newsItemElement.getAsJsonObject());
                    if (newsItem != null) {
                        addNews(newsItem);
                    }
                }
                firstCheck = false;
            } catch (Exception e) {
                if (logger.isDebug()) {
                    logger.error("Error while reading news item", e);
                }
            }
        } catch (JsonSyntaxException ignored) {}
    }

    public void handleNews(GeyserSession session, NewsItemAction action) {
        for (NewsItem news : getActiveNews(action)) {
            handleNewsItem(session, news, action);
        }
    }

    private void handleNewsItem(GeyserSession session, NewsItem news, NewsItemAction action) {
        switch (action) {
            case ON_SERVER_STARTED:
                if (!firstCheck) {
                    return;
                }
            case BROADCAST_TO_CONSOLE:
                logger.info(news.getMessage());
                break;
            case ON_OPERATOR_JOIN:
                //todo doesn't work, it's called before we know the op level.
//                if (session != null && session.getOpPermissionLevel() >= 2) {
//                    session.sendMessage(ChatColor.GREEN + news.getMessage());
//                }
                break;
            case BROADCAST_TO_OPERATORS:
                for (GeyserSession player : GeyserConnector.getInstance().getPlayers()) {
                    if (player.getOpPermissionLevel() >= 2) {
                        session.sendMessage(ChatColor.GREEN + news.getMessage());
                    }
                }
                break;
        }
    }

    public Collection<NewsItem> getActiveNews() {
        return activeNewsItems.values();
    }

    public Collection<NewsItem> getActiveNews(NewsItemAction action) {
        List<NewsItem> news = new ArrayList<>();
        for (NewsItem item : getActiveNews()) {
            if (item.getActions().contains(action)) {
                news.add(item);
            }
        }
        return news;
    }

    public void addNews(NewsItem item) {
        if (activeNewsItems.containsKey(item.getId())) {
            if (!item.isActive()) {
                activeNewsItems.remove(item.getId());
            }
            return;
        }

        if (!item.isActive()) {
            return;
        }

        if (!item.isGlobal() && !Constants.NEWS_PROJECT_NAME.equals(item.getProject())) {
            return;
        }

        switch (item.getType()) {
            case BUILD_SPECIFIC:
                if (!item.getDataAs(BuildSpecificData.class).isAffected(branch, build)) {
                    return;
                }
                break;
            case CHECK_AFTER:
                long checkAfter = item.getDataAs(CheckAfterData.class).getCheckAfter();
                long delayMs = System.currentTimeMillis() - checkAfter;
                schedule(delayMs > 0 ? delayMs : 0);
                break;
        }

        activeNewsItems.put(item.getId(), item);
        activateNews(item);
    }

    private void activateNews(NewsItem item) {
        for (NewsItemAction action : item.getActions()) {
            handleNewsItem(null, item, action);
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
