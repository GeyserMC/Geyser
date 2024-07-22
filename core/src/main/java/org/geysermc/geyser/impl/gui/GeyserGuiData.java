/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.impl.gui;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.HudElement;
import org.cloudburstmc.protocol.bedrock.data.HudVisibility;
import org.cloudburstmc.protocol.bedrock.packet.SetHudPacket;
import org.cloudburstmc.protocol.bedrock.packet.ToastRequestPacket;
import org.geysermc.geyser.api.bedrock.gui.GuiData;
import org.geysermc.geyser.api.bedrock.gui.GuiElement;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class GeyserGuiData implements GuiData {
    private static final HudElement[] HUD_ELEMENT_VALUES = HudElement.values();
    private static final Set<HudElement> ALL_HUD_ELEMENTS = Set.of(HUD_ELEMENT_VALUES);

    /**
     * An array of elements to hide when the player is in spectator mode.
     * Helps with tidying up the GUI; Java-style.
     */
    private static final GuiElement[] SPECTATOR_HIDDEN_ELEMENTS = {
            GuiElement.AIR_BUBBLES_BAR,
            GuiElement.ARMOR,
            GuiElement.HEALTH,
            GuiElement.FOOD_BAR,
            GuiElement.PROGRESS_BAR,
            GuiElement.TOOL_TIPS
    };

    private final GeyserSession session;

    public GeyserGuiData(GeyserSession session) {
        this.session = session;
    }

    /**
     * All currently hidden HUD elements
     */
    private final Set<GuiElement> hiddenHudElements = new HashSet<>();

    @Override
    public void hideElement(@NonNull GuiElement... elements) {
        Objects.requireNonNull(elements);
        SetHudPacket packet = new SetHudPacket();
        packet.setVisibility(HudVisibility.HIDE);
        Set<HudElement> elementSet = packet.getElements();

        for (GuiElement element : elements) {
            this.hiddenHudElements.add(element);
            elementSet.add(HUD_ELEMENT_VALUES[element.id()]);
        }

        session.sendUpstreamPacket(packet);
    }

    @Override
    public void resetElement(GuiElement... elements) {
        SetHudPacket packet = new SetHudPacket();
        packet.setVisibility(HudVisibility.RESET);
        Set<HudElement> elementSet = packet.getElements();

        if (elements != null && elements.length != 0) {
            for (GuiElement element : elements) {
                this.hiddenHudElements.remove(element);
                elementSet.add(HUD_ELEMENT_VALUES[element.id()]);
            }
        } else {
            this.hiddenHudElements.clear();
            elementSet.addAll(ALL_HUD_ELEMENTS);
        }

        session.sendUpstreamPacket(packet);
    }

    @Override
    public boolean isHudElementHidden(@NonNull GuiElement element) {
        Objects.requireNonNull(element);
        return this.hiddenHudElements.contains(element);
    }

    @Override
    public @NonNull Set<GuiElement> hiddenElements() {
        return Collections.unmodifiableSet(hiddenHudElements);
    }

    @Override
    public void sendToast(@NonNull String title, @NonNull String content) {
        ToastRequestPacket toastRequestPacket = new ToastRequestPacket();
        toastRequestPacket.setTitle(title);
        toastRequestPacket.setContent(content);
        session.sendUpstreamPacket(toastRequestPacket);
    }

    /**
     * Deals with hiding hud elements while in spectator.
     *
     * @param currentlySpectator whether the player is currently in spectator mode
     * @param newGameMode the new GameMode to switch to
     */
    public void handleGameModeChange(boolean currentlySpectator, GameMode newGameMode) {
        if (newGameMode == GameMode.SPECTATOR) {
            if (!currentlySpectator) {
                hideElement(SPECTATOR_HIDDEN_ELEMENTS);
            }
        } else {
            if (currentlySpectator) {
                resetElement(SPECTATOR_HIDDEN_ELEMENTS);
            }
        }
    }
}
