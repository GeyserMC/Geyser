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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import org.geysermc.geyser.configuration.GeyserJacksonConfiguration;

import java.io.File;
import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("FieldMayBeFinal") // Jackson requires that the fields are not final
public class GeyserViaProxyConfiguration extends GeyserJacksonConfiguration {

    private RemoteConfiguration remote = new RemoteConfiguration() {
        @Override
        public boolean isForwardHost() {
            return super.isForwardHost() || !ViaProxy.getConfig().getWildcardDomainHandling().equals(ViaProxyConfig.WildcardDomainHandling.NONE);
        }
    };

    @Override
    public Path getFloodgateKeyPath() {
        return new File(GeyserViaProxyPlugin.ROOT_FOLDER, this.getFloodgateKeyFile()).toPath();
    }

    @Override
    public int getPingPassthroughInterval() {
        int interval = super.getPingPassthroughInterval();
        if (interval < 15 && ViaProxy.getConfig().getTargetVersion() != null && ViaProxy.getConfig().getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
            // <= 1.6.4 servers sometimes block incoming connections from an IP address if too many connections are made
            interval = 15;
        }
        return interval;
    }

    @Override
    public RemoteConfiguration getRemote() {
        return this.remote;
    }

}
