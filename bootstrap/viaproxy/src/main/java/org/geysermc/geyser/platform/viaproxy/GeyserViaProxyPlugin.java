/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.viaproxy;

import net.lenni0451.lambdaevents.EventHandler;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStartEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStopEvent;
import net.raphimc.viaproxy.plugins.events.ShouldVerifyOnlineModeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import java.io.File;
import java.util.UUID;

public class GeyserViaProxyPlugin extends ViaProxyPlugin {

    public static final Logger LOGGER = LogManager.getLogger("Geyser");
    public static final File ROOT_FOLDER = new File(PluginManager.PLUGINS_DIR, "Geyser");

    private GeyserViaProxyBootstrap bootstrap;

    @Override
    public void onEnable() {
        ROOT_FOLDER.mkdirs();

        this.bootstrap = new GeyserViaProxyBootstrap(LOGGER, ROOT_FOLDER);
        GeyserLocale.init(this.bootstrap);
        this.bootstrap.onGeyserInitialize();

        ViaProxy.EVENT_MANAGER.register(this);
    }

    @EventHandler
    private void onConsoleCommand(final ConsoleCommandEvent event) {
        final String command = event.getCommand().startsWith("/") ? event.getCommand().substring(1) : event.getCommand();
        if (this.bootstrap.getGeyserCommandManager().runCommand(this.bootstrap.getGeyserLogger(), command + " " + String.join(" ", event.getArgs()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onShouldVerifyOnlineModeEvent(final ShouldVerifyOnlineModeEvent event) {
        final UUID uuid = event.getProxyConnection().getGameProfile().getId();
        if (uuid == null) return;

        final GeyserSession connection = GeyserImpl.getInstance().onlineConnections().stream().filter(s -> s.javaUuid().equals(uuid)).findAny().orElse(null);
        if (connection == null) return;

        if (connection.javaUsername().equals(event.getProxyConnection().getGameProfile().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onProxyStart(final ProxyStartEvent event) {
        this.bootstrap.onGeyserEnable();
    }

    @EventHandler
    private void onProxyStop(final ProxyStopEvent event) {
        GeyserImpl.getInstance().getSessionManager().disconnectAll("geyser.commands.reload.kick");
        this.bootstrap.onGeyserDisable();
    }

}
