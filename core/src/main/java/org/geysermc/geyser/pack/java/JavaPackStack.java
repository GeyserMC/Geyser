/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.pack.java;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.mcprotocollib.protocol.data.game.ResourcePackStatus;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPopPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;

import java.util.List;
import java.util.function.Consumer;

public final class JavaPackStack {
    private final List<JavaPack> stack = new ObjectArrayList<>();
    @Getter
    @Accessors(fluent = true)
    private JavaPack.Composed composed = JavaPack.Composed.EMPTY;

    public void pushPack(ClientboundResourcePackPushPacket packet, Consumer<ResourcePackStatus> statusConsumer) {
        JavaPackManager.getInstance().downloadIfAbsent(packet.getId(), packet.getUrl(), packet.getHash(), statusConsumer)
            .thenAccept(result -> result.ifPresent(pack -> {
                synchronized (this) {
                    stack.add(pack);
                    reload();
                }
            }));
    }

    public void popPack(ClientboundResourcePackPopPacket packet) {
        synchronized (this) {
            if (packet.getId() != null) {
                stack.removeIf(pack -> pack.id().uuid().equals(packet.getId()));
            } else {
                stack.clear();
            }
            reload();
        }
    }

    private void reload() {
        GeyserImpl.getInstance().getLogger().info("Reloading Java resourcepack stack");
        composed = JavaPack.Composed.composePacks(stack.stream().map(JavaPack::contents));
    }
}
