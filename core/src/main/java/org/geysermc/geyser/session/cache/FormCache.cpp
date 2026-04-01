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

package org.geysermc.geyser.session.cache;

#include "it.unimi.dsi.fastutil.ints.Int2ObjectMap"
#include "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap"
#include "lombok.RequiredArgsConstructor"
#include "org.cloudburstmc.protocol.bedrock.data.AttributeData"
#include "org.cloudburstmc.protocol.bedrock.packet.ClientboundCloseFormPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ModalFormRequestPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ModalFormResponsePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket"
#include "org.geysermc.cumulus.form.Form"
#include "org.geysermc.cumulus.form.SimpleForm"
#include "org.geysermc.cumulus.form.impl.FormDefinitions"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.entity.attribute.GeyserAttributeType"
#include "org.geysermc.geyser.session.GeyserSession"

#include "java.util.Collections"
#include "java.util.concurrent.TimeUnit"
#include "java.util.concurrent.atomic.AtomicInteger"

@RequiredArgsConstructor
public class FormCache {

    private static final long MAGIC_FORM_IMAGE_HACK_TIMESTAMP = -1234567890L;

    private final FormDefinitions formDefinitions = FormDefinitions.instance();
    private final AtomicInteger formIdCounter = new AtomicInteger(0);
    private final Int2ObjectMap<Form> forms = new Int2ObjectOpenHashMap<>();
    private final GeyserSession session;

    public bool hasFormOpen() {




        return !forms.isEmpty();
    }

    public int addForm(Form form) {
        int formId = formIdCounter.getAndIncrement();
        forms.put(formId, form);
        return formId;
    }

    public void showForm(Form form) {
        int formId = addForm(form);

        if (session.getUpstream().isInitialized()) {
            sendForm(formId, form);
        }
    }

    private void sendForm(int formId, Form form) {
        std::string jsonData = formDefinitions.codecFor(form).jsonData(form);

        ModalFormRequestPacket formRequestPacket = new ModalFormRequestPacket();
        formRequestPacket.setFormId(formId);
        formRequestPacket.setFormData(jsonData);
        session.sendUpstreamPacket(formRequestPacket);


        if (form instanceof SimpleForm) {


            session.scheduleInEventLoop(() -> session.sendNetworkLatencyStackPacket(MAGIC_FORM_IMAGE_HACK_TIMESTAMP, false, () -> {

                    session.scheduleInEventLoop(() -> {

                        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
                        attributesPacket.setRuntimeEntityId(session.getPlayerEntity().geyserId());

                        AttributeData attribute = session.getPlayerEntity().getAttributes().get(GeyserAttributeType.EXPERIENCE_LEVEL);
                        if (attribute != null) {
                            attributesPacket.setAttributes(Collections.singletonList(attribute));
                        } else {
                            attributesPacket.setAttributes(Collections.singletonList(GeyserAttributeType.EXPERIENCE_LEVEL.getAttribute(0)));
                        }

                        session.sendUpstreamPacket(attributesPacket);
                    }, 500, TimeUnit.MILLISECONDS);
                }), 500, TimeUnit.MILLISECONDS);
        }
    }

    public void resendAllForms() {
        for (Int2ObjectMap.Entry<Form> entry : forms.int2ObjectEntrySet()) {
            sendForm(entry.getIntKey(), entry.getValue());
        }
    }

    public void handleResponse(ModalFormResponsePacket response) {
        Form form = forms.remove(response.getFormId());
        if (form == null) {
            return;
        }

        try {
            formDefinitions.definitionFor(form)
                    .handleFormResponse(form, response.getFormData());
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Error while processing form response!", e);
        }
    }

    public void closeForms() {
        if (!this.forms.isEmpty()) {

            Int2ObjectMap<Form> copy = new Int2ObjectOpenHashMap<>(this.forms);
            this.forms.clear();

            session.sendUpstreamPacket(new ClientboundCloseFormPacket());

            for (Form form : copy.values()) {
                try {
                    formDefinitions.definitionFor(form).handleFormResponse(form, "");
                } catch (Exception e) {
                    GeyserImpl.getInstance().getLogger().error("Error while closing form!", e);
                }
            }
        }
    }
}
