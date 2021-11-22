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

package org.geysermc.connector;

import com.nukkitx.protocol.bedrock.BedrockServer;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.api.Geyser;

import java.util.UUID;

/**
 * Deprecated, please use {@link Geyser} or {@link GeyserImpl}.
 *
 * @deprecated legacy code
 */
@Deprecated
public class GeyserConnector {
    public static final String NAME = GeyserImpl.NAME;
    public static final String GIT_VERSION = GeyserImpl.GIT_VERSION; // A fallback for running in IDEs
    public static final String VERSION = GeyserImpl.VERSION; // A fallback for running in IDEs

    public static final String OAUTH_CLIENT_ID = GeyserImpl.OAUTH_CLIENT_ID;

    private static final GeyserConnector INSTANCE = new GeyserConnector();

    public static GeyserConnector getInstance() {
        return INSTANCE;
    }

    public BedrockServer getBedrockServer() {
        return GeyserImpl.getInstance().getBedrockServer();
    }

    public  boolean isShuttingDown() {
        return GeyserImpl.getInstance().isShuttingDown();
    }

    public PlatformType getPlatformType() {
        return GeyserImpl.getInstance().getPlatformType();
    }

    public void shutdown() {
        GeyserImpl.getInstance().shutdown();
    }

    public void reload() {
        GeyserImpl.getInstance().reload();
    }

    public GeyserSession getPlayerByXuid(String xuid) {
        org.geysermc.geyser.session.GeyserSession session = GeyserImpl.getInstance().connectionByXuid(xuid);
        if (session != null) {
            return new GeyserSession(session);
        } else {
            return null;
        }
    }

    public GeyserSession getPlayerByUuid(UUID uuid) {
        org.geysermc.geyser.session.GeyserSession session = GeyserImpl.getInstance().connectionByUuid(uuid);
        if (session != null) {
            return new GeyserSession(session);
        } else {
            return null;
        }
    }

    public boolean isProductionEnvironment() {
        return GeyserImpl.getInstance().productionEnvironment();
    }
}
