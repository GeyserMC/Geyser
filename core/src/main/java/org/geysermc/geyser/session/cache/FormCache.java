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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.packet.ModalFormRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.ModalFormResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.cumulus.component.util.ComponentType;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.impl.FormDefinitions;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class FormCache {
    private static final Gson GSON_TEMP = new Gson();

    /**
     * The magnitude of this doesn't actually matter, but it must be negative so that
     * BedrockNetworkStackLatencyTranslator can detect the hack.
     */
    private static final long MAGIC_FORM_IMAGE_HACK_TIMESTAMP = -1234567890L;

    private final FormDefinitions formDefinitions = FormDefinitions.instance();
    private final AtomicInteger formIdCounter = new AtomicInteger(0);
    private final Int2ObjectMap<Form> forms = new Int2ObjectOpenHashMap<>();
    private final GeyserSession session;

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
        String jsonData = formDefinitions.codecFor(form).jsonData(form);

        ModalFormRequestPacket formRequestPacket = new ModalFormRequestPacket();
        formRequestPacket.setFormId(formId);
        formRequestPacket.setFormData(jsonData);
        session.sendUpstreamPacket(formRequestPacket);

        // Hack to fix the (url) image loading bug
        if (form instanceof SimpleForm) {
            NetworkStackLatencyPacket latencyPacket = new NetworkStackLatencyPacket();
            latencyPacket.setFromServer(true);
            latencyPacket.setTimestamp(MAGIC_FORM_IMAGE_HACK_TIMESTAMP);
            session.scheduleInEventLoop(
                    () -> {
                        session.getLatencyPingCache().add(() -> session.scheduleInEventLoop(() -> {
                            // Hack to fix the url image loading bug
                            UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
                            attributesPacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());

                            AttributeData attribute = session.getPlayerEntity().getAttributes().get(GeyserAttributeType.EXPERIENCE_LEVEL);
                            if (attribute != null) {
                                attributesPacket.setAttributes(Collections.singletonList(attribute));
                            } else {
                                attributesPacket.setAttributes(Collections.singletonList(GeyserAttributeType.EXPERIENCE_LEVEL.getAttribute(0)));
                            }

                            session.sendUpstreamPacket(attributesPacket);
                        }, 500, TimeUnit.MILLISECONDS));
                        session.sendUpstreamPacket(latencyPacket);
                    },
                    500, TimeUnit.MILLISECONDS
            );
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

        String responseData = response.getFormData();
        //todo work on a proper solution in Cumulus, but that'd require all Floodgate instances to update as well and
        // drops support for older Bedrock versions (because Cumulus isn't made to support multiple versions). That's
        // why this hotfix exists.
        if (form instanceof CustomForm customForm && GameProtocol.isTheOneVersionWithBrokenForms(session) && response.getCancelReason().isEmpty()) {
            // Labels are no longer included as a json null, so we have to manually add them for now.
            IntList labelIndexes = new IntArrayList();
            for (int i = 0; i < customForm.content().size(); i++) {
                var component = customForm.content().get(i);
                if (component == null) {
                    continue;
                }
                if (component.type() == ComponentType.LABEL) {
                    labelIndexes.add(i);
                }
            }
            if (!labelIndexes.isEmpty()) {
                // If the form only has labels, the response is the literal
                // null (with a newline char) instead of a json array
                if (responseData.startsWith("null")) {
                    List<Object> newResponse = new ArrayList<>();
                    for (int i = 0; i < labelIndexes.size(); i++) {
                        newResponse.add(null);
                    }
                    responseData = GSON_TEMP.toJson(newResponse);
                } else {
                    JsonArray responseDataArray = GSON_TEMP.fromJson(responseData, JsonArray.class);
                    List<Object> newResponse = new ArrayList<>();

                    int handledLabelCount = 0;
                    for (int i = 0; i < responseDataArray.size() + labelIndexes.size(); i++) {
                        if (labelIndexes.contains(i)) {
                            newResponse.add(null);
                            handledLabelCount++;
                            continue;
                        }
                        newResponse.add(responseDataArray.get(i - handledLabelCount));
                    }
                    responseData = GSON_TEMP.toJson(newResponse);
                }
            }
        }

        try {
            formDefinitions.definitionFor(form)
                    .handleFormResponse(form, responseData);
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Error while processing form response!", e);
        }
    }
}
