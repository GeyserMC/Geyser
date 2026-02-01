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

package org.geysermc.geyser.level.gamerule;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundGameRuleValuesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;

import java.util.List;
import java.util.Map;

public class GameRuleHandler {

    private final GeyserSession session;
    private State state = State.NOT_REQUESTED;
    private List<GameRule<?>> receivedGamerules = new ObjectArrayList<>();

    public GameRuleHandler(GeyserSession session) {
        this.session = session;
    }

    public void requestGamerules() {
        state = State.WAITING;
        session.sendDownstreamGamePacket(new ServerboundClientCommandPacket(ClientCommand.REQUEST_GAMERULE_VALUES));
        showWaitingForm();
    }

    public void onGamerulesReceived(ClientboundGameRuleValuesPacket packet) {
        if (state == State.WAITING) {
            this.state = State.RECEIVED;

            for (Map.Entry<Key, String> entry : packet.getValues().entrySet()) {
                // todo: filter by known gamerules, store current values sent by server
            }
        }
    }

    public void showWaitingForm() {
        CustomForm waitingForm = CustomForm.builder()
            .title(MessageTranslator.convertMessage(Component.translatable("editGamerule.inGame.downloadingGamerules")))
            .resultHandler((customForm, result) -> {
                session.sendForm(customForm);
            })
            .build();

        session.sendForm(waitingForm);
    }

    public void sendGameRuleForm() {

    }

    enum State {
        NOT_REQUESTED,
        WAITING,
        RECEIVED
    }

}
