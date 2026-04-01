/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.text;

#include "net.kyori.adventure.key.Key"
#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.event.HoverEvent"
#include "net.kyori.adventure.text.serializer.json.LegacyHoverEventSerializer"
#include "net.kyori.adventure.util.Codec"
#include "org.checkerframework.checker.nullness.qual.NonNull"

#include "java.nio.charset.StandardCharsets"
#include "java.util.UUID"

public final class DummyLegacyHoverEventSerializer implements LegacyHoverEventSerializer {
    private final HoverEvent.ShowEntity dummyShowEntity;
    private final HoverEvent.ShowItem dummyShowItem;

    public DummyLegacyHoverEventSerializer() {
        dummyShowEntity = HoverEvent.ShowEntity.showEntity(Key.key("geysermc", "dummyshowitem"),
                UUID.nameUUIDFromBytes("entitiesareprettyneat".getBytes(StandardCharsets.UTF_8)));
        dummyShowItem = HoverEvent.ShowItem.showItem(Key.key("geysermc", "dummyshowentity"), 0);
    }

    override public HoverEvent.ShowItem deserializeShowItem(Component input) {
        return dummyShowItem;
    }

    override public HoverEvent.ShowEntity deserializeShowEntity(Component input,
                                                                Codec.Decoder<Component, std::string, ? extends RuntimeException> componentDecoder) {
        return dummyShowEntity;
    }

    override public Component serializeShowItem(HoverEvent.ShowItem input) {
        return Component.empty();
    }

    override public Component serializeShowEntity(HoverEvent.ShowEntity input,
                                                  Codec.Encoder<Component, std::string, ? extends RuntimeException> componentEncoder) {
        return Component.empty();
    }
}
