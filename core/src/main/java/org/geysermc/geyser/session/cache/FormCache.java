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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.ClientboundCloseFormPacket;
import org.cloudburstmc.protocol.bedrock.packet.ModalFormRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.ModalFormResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.NpcDialoguePacket;
import org.cloudburstmc.protocol.bedrock.packet.NpcRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.NpcForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.impl.FormDefinitions;
import org.geysermc.cumulus.form.impl.npc.NpcFormImpl;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.player.MannequinEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class FormCache {
    /**
     * The magnitude of this doesn't actually matter, but it must be negative so that
     * BedrockNetworkStackLatencyTranslator can detect the hack.
     */
    private static final long MAGIC_FORM_IMAGE_HACK_TIMESTAMP = -1234567890L;

    private final FormDefinitions formDefinitions = FormDefinitions.instance();
    private final AtomicInteger formIdCounter = new AtomicInteger(0);
    private final Int2ObjectMap<Form> forms = new Int2ObjectOpenHashMap<>();
    private final GeyserSession session;

    public boolean hasFormOpen() {
        // If forms is empty it implies that there are no forms to show
        // so technically this returns "has forms to show" or "has open"
        // Forms are only queued in specific circumstances, such as waiting on
        // previous inventories to close
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
        String jsonData = formDefinitions.codecFor(form).jsonData(form);

        //Npc forms need to be handled differently
        if (form instanceof NpcFormImpl npcForm) {
            NpcDialoguePacket npcDialoguePacket = new NpcDialoguePacket();
            npcDialoguePacket.setNpcName(npcForm.title());
            npcDialoguePacket.setDialogue(npcForm.content());
            npcDialoguePacket.setSceneName(""+formId); //This isn't visible to the player, so we're using this for formID
            npcDialoguePacket.setAction(NpcDialoguePacket.Action.OPEN);

            //Add NPC data
            Entity entity = (Entity) session.entities().byUuid(npcForm.entityUUID());
            if (entity == null) {
                closeForms();
                throw new NullPointerException("NpcForm Entity is null!");
            }

            //sanity check
            if (!(entity instanceof LivingEntity)) {
                closeForms();
                throw new IllegalArgumentException("NpcForm Enity is not a LivingEntity!");
            }

            entity.getMetadata().put(EntityDataTypes.HAS_NPC, true);
            entity.getMetadata().put(EntityDataTypes.NPC_DATA, npcForm.npc_data);
            entity.updateBedrockMetadata();
            session.setNpcId(entity.geyserId());

            npcDialoguePacket.setUniqueEntityId(entity.geyserId());
            String actionJson = npcForm.actionJson;
            npcDialoguePacket.setActionJson(actionJson);

            //a delay is needed here, otherwise the dialogue wont open
            session.scheduleInEventLoop(() -> {
                session.sendUpstreamPacket(npcDialoguePacket);
            }, 200, TimeUnit.MILLISECONDS);
        } else {
            ModalFormRequestPacket formRequestPacket = new ModalFormRequestPacket();
            formRequestPacket.setFormId(formId);
            formRequestPacket.setFormData(jsonData);
            session.sendUpstreamPacket(formRequestPacket);

            // Hack to fix the (url) image loading bug
            if (form instanceof SimpleForm) {
                // Two delays:
                // First, 500ms, before we send the network stack latency packet
                session.scheduleInEventLoop(() -> session.sendNetworkLatencyStackPacket(MAGIC_FORM_IMAGE_HACK_TIMESTAMP, false, () -> {
                    // Then, wait 500ms after we receive the response, then update attributes to get the image to show
                    session.scheduleInEventLoop(() -> {
                        // Hack to fix the url image loading bug
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
    }

    public void resendAllForms() {
        for (Int2ObjectMap.Entry<Form> entry : forms.int2ObjectEntrySet()) {
            sendForm(entry.getIntKey(), entry.getValue());
        }
    }

    //Npc forms need to be handled differently
    public void handleResponse(NpcRequestPacket response) {
        Form form = forms.remove(Integer.parseInt(response.getSceneName()));
        if (form == null) {
            return;
        }

        try {
            formDefinitions.definitionFor(form)
                .handleFormResponse(form, ""+response.getActionType());
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Error while processing form response!", e);
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
            // Copy them to ensure any response handler's sent form isn't instantly cleared
            Int2ObjectMap<Form> copy = new Int2ObjectOpenHashMap<>(this.forms);
            this.forms.clear();

            // Now close it
            // Npc forms need to be handled differently, There is no way to tell which type of form is open,
            // so we're sending both types of closing packets here to make sure either type gets closed
            NpcDialoguePacket npcDialoguePacket = new NpcDialoguePacket();
            npcDialoguePacket.setAction(NpcDialoguePacket.Action.CLOSE);
            npcDialoguePacket.setSceneName(""+formIdCounter.get()); //assuming it matches up with what was sent
            npcDialoguePacket.setDialogue("");
            npcDialoguePacket.setActionJson("");
            npcDialoguePacket.setNpcName("");
            npcDialoguePacket.setUniqueEntityId(session.getNpcId());
            session.sendUpstreamPacket(npcDialoguePacket);

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
