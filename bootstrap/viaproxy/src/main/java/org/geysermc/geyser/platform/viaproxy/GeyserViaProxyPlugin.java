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
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStartEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStopEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.text.GeyserLocale;

import java.io.File;

public class GeyserViaProxyPlugin extends ViaProxyPlugin {

    public static final Logger LOGGER = LogManager.getLogger("Geyser");
    public static final File ROOT_FOLDER = new File(PluginManager.PLUGINS_DIR, "Geyser");

    private GeyserViaProxyBootstrap bootstrap;

    @Override
    public void onEnable() {
        ROOT_FOLDER.mkdirs();

        this.bootstrap = new GeyserViaProxyBootstrap();
        GeyserLocale.init(this.bootstrap);
        this.bootstrap.onEnable();
        GeyserImpl.getInstance().shutdown();

        PluginManager.EVENT_MANAGER.register(this);
    }

    @EventHandler
    public void onConsoleCommand(final ConsoleCommandEvent event) {
        if (event.getCommand().equals("geyser") || event.getCommand().equals("/geyser")) {
            event.setCancelled(true);

            this.bootstrap.getGeyserCommandManager().runCommand(this.bootstrap.getGeyserLogger(), "geyser " + String.join(" ", event.getArgs()));
        }
    }

    // Code below split apart from GeyserImpl.getInstance().reload();

    @EventHandler
    public void onProxyStart(final ProxyStartEvent event) {
        GeyserImpl.getInstance().extensionManager().enableExtensions();
        this.bootstrap.onEnable();
    }

    @EventHandler
    public void onProxyStop(final ProxyStopEvent event) {
        GeyserImpl.getInstance().shutdown();
    }

}
