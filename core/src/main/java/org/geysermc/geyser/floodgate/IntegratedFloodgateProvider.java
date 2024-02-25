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

import io.micronaut.core.type.Argument;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.netty.util.AttributeKey;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.FloodgatePlatform;
import org.geysermc.floodgate.core.skin.SkinApplier;
import org.geysermc.floodgate.core.skin.SkinDataImpl;
import org.geysermc.geyser.session.GeyserSession;

public class IntegratedFloodgateProvider implements FloodgateProvider {
    public static final AttributeKey<Connection> SESSION_KEY = AttributeKey.valueOf("floodgate-player");

    private final SkinApplier skinApplier;

    public IntegratedFloodgateProvider(FloodgatePlatform platform) {
        skinApplier = platform.getBean(SkinApplier.class);

        var connectionAttribute = platform.getBean(
                Argument.of(AttributeKey.class, Connection.class),
                Qualifiers.byName("connectionAttribute")
        );
        if (connectionAttribute.id() != SESSION_KEY.id()) {
            throw new IllegalStateException("Session key doesn't match Floodgate's key!");
        }
    }

    @Override
    public void onSkinUpload(GeyserSession session, String value, String signature) {
        skinApplier.applySkin(session, new SkinDataImpl(value, signature));
    }

    @Override
    public String onClientIntention(GeyserSession session) {
        // we don't have to do anything here, it's done in LocalServerChannelWrapper
        return null;
    }
}
