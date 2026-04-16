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

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundGameRuleValuesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundSetGameRulePacket;

import java.util.Map;
import java.util.Objects;

public class GameRuleHandler {

    private final GeyserSession session;
    @Getter
    private State state = State.NOT_REQUESTED;
    private final Map<GameRule<?>, String> currentGamerules = new Object2ObjectArrayMap<>();

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
            this.state = State.SHOWN;
            this.currentGamerules.clear();

            for (Map.Entry<Key, String> entry : packet.getValues().entrySet()) {
                GameRule<?> gameRule = Registries.GAME_RULES.get(entry.getKey());
                if (gameRule == null) {
                    continue;
                }
                currentGamerules.put(gameRule, entry.getValue());
            }

            sendGameRuleForm();
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
        CustomForm.Builder builder = CustomForm.builder();

        for (Map.Entry<GameRule<?>, String> entry : currentGamerules.entrySet()) {
            builder.component(entry.getKey().toComponent(entry.getValue()));
        }

        builder.validResultHandler((customForm, result) -> {
            Map<Key, String> responses = new Object2ObjectArrayMap<>();
            for (Map.Entry<GameRule<?>, String> entry : currentGamerules.entrySet()) {
                handleUpdate(entry.getKey(), entry.getValue(), result.next(), responses);
            }
            if (session.getOpPermissionLevel() >= 2) {
                session.sendDownstreamGamePacket(new ServerboundSetGameRulePacket(responses));
            }
            state = State.NOT_REQUESTED;
            currentGamerules.clear();
        });

        builder.closedOrInvalidResultHandler((customForm, result) -> {
           this.state = State.NOT_REQUESTED;
           this.currentGamerules.clear();
        });

        session.sendForm(builder);
    }

    public <T> void handleUpdate(GameRule<T> gameRule, String previous, Object newValue, Map<Key, String> responses) {
        T value;
        try {
            value = gameRule.adapter().parser().apply(newValue);
        } catch (Throwable e) {
            GeyserImpl.getInstance().getLogger().debug("Got invalid value for gamerule %s (old value: %s, new value: %s)", gameRule.key(), previous, newValue);
            return;
        }

        if (!gameRule.validate(value)) {
            GeyserImpl.getInstance().getLogger().debug("Got invalid value for gamerule %s (old value: %s, new value: %s)", gameRule.key(), previous, newValue);
            return;
        }

        T oldValue = gameRule.adapter().parser().apply(previous);
        if (Objects.equals(oldValue, value)) {
            return;
        }

        responses.put(gameRule.key(), value.toString());
    }

    public enum State {
        NOT_REQUESTED,
        WAITING,
        SHOWN
    }

}
