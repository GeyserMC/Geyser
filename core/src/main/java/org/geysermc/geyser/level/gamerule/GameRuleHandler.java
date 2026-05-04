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
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundGameRuleValuesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundSetGameRulePacket;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class GameRuleHandler {

    private static final Component EDIT_TITLE = Component.translatable("editGamerule.title");
    private final GeyserSession session;
    @Getter
    private State state = State.NOT_REQUESTED;

    public GameRuleHandler(GeyserSession session) {
        this.session = session;
    }

    public void requestGamerules() {
        state = State.WAITING;
        // Hacky, but otherwise, the waiting form shows up *after* the edit form if the server responds too quickly
        session.scheduleInEventLoop(() -> {
            if (state == State.WAITING) {
                showWaitingForm();
            }
        }, 500, TimeUnit.MILLISECONDS);
        session.sendDownstreamGamePacket(new ServerboundClientCommandPacket(ClientCommand.REQUEST_GAMERULE_VALUES));
    }

    public void onGamerulesReceived(ClientboundGameRuleValuesPacket packet) {
        if (state == State.WAITING) {
            state = State.SHOWN;

            Map<GameRuleCategory, Map<GameRule<?>, String>> values = new Object2ObjectArrayMap<>();
            for (Map.Entry<Key, String> entry : packet.getValues().entrySet()) {
                GameRule<?> gameRule = Registries.GAME_RULES.get(entry.getKey());
                if (gameRule == null) {
                    GeyserImpl.getInstance().getLogger().debug("Unknown gamerule: " + entry.getKey());
                    continue;
                }
                values.computeIfAbsent(gameRule.category(), (category) -> new Object2ObjectArrayMap<>()).put(gameRule, entry.getValue());
            }

            CustomForm.Builder builder = CustomForm.builder();
            builder.title(MessageTranslator.convertMessage(EDIT_TITLE, session.locale()));

            Map<GameRule<?>, String> ordered = new Object2ObjectArrayMap<>(packet.getValues().size());
            values.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRuleCategory::id)))
                .forEach(entry -> {
                    builder.label(MessageTranslator.convertMessage(entry.getKey().label(), session.locale()));

                    entry.getValue().entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRule::key)))
                        .forEach(nested -> {
                            builder.component(nested.getKey().toComponent(session, nested.getValue()));
                            ordered.put(nested.getKey(), nested.getValue());
                        });
                });

            builder.validResultHandler((customForm, result) -> {
                Map<Key, String> responses = new Object2ObjectArrayMap<>();
                for (var entry : ordered.entrySet()) {
                    handleUpdate(entry.getKey(), entry.getValue(), result.next(), responses);
                }
                if (session.getOpPermissionLevel() >= 2 && !responses.isEmpty()) {
                    session.sendDownstreamGamePacket(new ServerboundSetGameRulePacket(responses));
                }
                state = State.NOT_REQUESTED;
            });
            builder.closedOrInvalidResultHandler((customForm, result) -> state = State.NOT_REQUESTED);

            session.sendForm(builder);
        }
    }

    public void showWaitingForm() {
        CustomForm waitingForm = CustomForm.builder()
            .title("editGamerule.inGame.downloadingGamerules")
            .translator(MinecraftLocale::getLocaleString, session.locale())
            .validResultHandler((customForm, result) -> {
                // Resend waiting form only on submit; allow closing
                showWaitingForm();
            })
            .closedOrInvalidResultHandler((customForm, result) -> {
                if (state != State.SHOWN) {
                    this.state = State.NOT_REQUESTED;
                }
            })
            .build();
        session.sendForm(waitingForm);
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
