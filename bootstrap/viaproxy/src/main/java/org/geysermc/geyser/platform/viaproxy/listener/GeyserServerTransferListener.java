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

package org.geysermc.geyser.platform.viaproxy.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.HostAndPort;
import org.geysermc.event.PostOrder;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.java.ServerTransferEvent;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GeyserServerTransferListener {

    private final Cache<String, Map<String, byte[]>> cookieStorages = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Subscribe(postOrder = PostOrder.FIRST)
    private void onServerTransfer(final ServerTransferEvent event) {
        this.cookieStorages.put(event.connection().xuid(), event.cookies());
        final GeyserSession geyserSession = (GeyserSession) event.connection();
        final HostAndPort hostAndPort = HostAndPort.fromString(geyserSession.getClientData().getServerAddress()).withDefaultPort(19132);
        event.bedrockHost(hostAndPort.getHost());
        event.bedrockPort(hostAndPort.getPort());
    }

    @Subscribe(postOrder = PostOrder.FIRST)
    private void onSessionLogin(final SessionLoginEvent event) {
        final Map<String, byte[]> cookies = this.cookieStorages.asMap().remove(event.connection().xuid());
        if (cookies != null) {
            event.cookies(cookies);
            event.transferring(true);
        }
    }

}
