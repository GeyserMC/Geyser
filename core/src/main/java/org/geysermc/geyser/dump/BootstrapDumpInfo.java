/*
 * Copyright (c) 2020-2024 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.dump;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.text.AsteriskSerializer;

import java.util.List;

@Getter
public class BootstrapDumpInfo {
    private final PlatformType platform;

    public BootstrapDumpInfo() {
        this.platform = GeyserImpl.getInstance().getPlatformType();
    }

    @Getter
    @AllArgsConstructor
    public static class PluginInfo {
        public boolean enabled;
        public String name;
        public String version;
        public String main;
        public List<String> authors;
    }

    @Getter
    @AllArgsConstructor
    public static class ListenerInfo {

        @AsteriskSerializer.Asterisk(isIp = true)
        public String ip;
        public int port;
    }
}
