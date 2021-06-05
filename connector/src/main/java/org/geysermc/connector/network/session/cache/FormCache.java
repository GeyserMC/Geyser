/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.session.cache;

import com.nukkitx.protocol.bedrock.packet.ModalFormRequestPacket;
import com.nukkitx.protocol.bedrock.packet.ModalFormResponsePacket;
import com.nukkitx.protocol.bedrock.packet.NetworkStackLatencyPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.SimpleForm;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class FormCache {
    private final AtomicInteger formId = new AtomicInteger(0);
    private final Int2ObjectMap<Form> forms = new Int2ObjectOpenHashMap<>();
    private final GeyserSession session;

    public int addForm(Form form) {
        int windowId = formId.getAndIncrement();
        forms.put(windowId, form);
        return windowId;
    }

    public int showForm(Form form) {
        int windowId = addForm(form);

        ModalFormRequestPacket formRequestPacket = new ModalFormRequestPacket();
        formRequestPacket.setFormId(windowId);
        formRequestPacket.setFormData(form.getJsonData());
        session.sendUpstreamPacket(formRequestPacket);

        // Hack to fix the (url) image loading bug
        if (form instanceof SimpleForm) {
            NetworkStackLatencyPacket latencyPacket = new NetworkStackLatencyPacket();
            latencyPacket.setFromServer(true);
            latencyPacket.setTimestamp(-System.currentTimeMillis());
            session.getConnector().getGeneralThreadPool().schedule(
                    () -> session.sendUpstreamPacket(latencyPacket),
                    500, TimeUnit.MILLISECONDS);
        }

        return windowId;
    }

    public void handleResponse(ModalFormResponsePacket response) {
        Form form = forms.get(response.getFormId());
        if (form == null) {
            return;
        }

        Consumer<String> responseConsumer = form.getResponseHandler();
        if (responseConsumer != null) {
            responseConsumer.accept(response.getFormData());
        }

        removeWindow(response.getFormId());
    }

    public boolean removeWindow(int id) {
        return forms.remove(id) != null;
    }
}
