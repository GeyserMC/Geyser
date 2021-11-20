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

package org.geysermc.geyser.translator.protocol.java;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSelectAdvancementsTabPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.AdvancementsCache;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

/**
 * Indicates that the client should open a particular advancement tab
 */
@Translator(packet = ClientboundSelectAdvancementsTabPacket.class)
public class JavaSelectAdvancementsTabTranslator extends PacketTranslator<ClientboundSelectAdvancementsTabPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSelectAdvancementsTabPacket packet) {
        AdvancementsCache advancementsCache = session.getAdvancementsCache();
        advancementsCache.setCurrentAdvancementCategoryId(packet.getTabId());
        advancementsCache.buildAndShowListForm();
    }
}
