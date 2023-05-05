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

package org.geysermc.geyser.hybrid;

import io.netty.util.AttributeKey;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;

public class IntegratedHybridProvider implements HybridProvider {
    // TODO This will probably end up as its own class.
    public static final AttributeKey<GeyserSession> SESSION_KEY = AttributeKey.valueOf("geyser-session");

    private final SkinApplier skinApplier;

    public IntegratedHybridProvider(GeyserImpl geyser) {
        skinApplier = geyser.getBootstrap().createSkinApplier();
    }

    @Override
    public void onSkinUpload(GeyserSession session, String value, String signature) {
        skinApplier.applySkin(session, new SkinData(value, signature));
    }

    @Override
    public FloodgateCipher getCipher() {
        throw new UnsupportedOperationException();
    }
}
