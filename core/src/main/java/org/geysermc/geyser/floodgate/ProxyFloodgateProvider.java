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

package org.geysermc.geyser.floodgate;

import java.nio.charset.StandardCharsets;
import org.geysermc.floodgate.core.FloodgatePlatform;
import org.geysermc.floodgate.core.crypto.FloodgateDataCodec;
import org.geysermc.geyser.session.GeyserSession;

//todo Floodgate should be responsible for forwarding its messages
public final class ProxyFloodgateProvider implements FloodgateProvider {
    private final FloodgateDataCodec dataCodec;

    public ProxyFloodgateProvider(FloodgatePlatform platform) {
        dataCodec = platform.getBean(FloodgateDataCodec.class);
    }

    @Override
    public void onSkinUpload(GeyserSession session, String value, String signature) {
        byte[] bytes = (value + '\0' + signature)
                .getBytes(StandardCharsets.UTF_8);
        //todo
//        PluginMessageUtils.sendMessage(session, PluginMessageChannels.SKIN, bytes);
    }

    @Override
    public String onClientIntention(GeyserSession session) throws Exception {
        return dataCodec.encodeToString(session);
    }
}
