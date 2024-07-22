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

package org.geysermc.geyser.api.bedrock.gui;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

public interface GuiData {
    /**
     * Hides a {@link GuiElement} on the client's side.
     *
     * @param element the {@link GuiElement} to hide
     */
    void hideElement(@NonNull GuiElement... element);

    /**
     * Resets a {@link GuiElement} on the client's side.
     * This makes the client decide on its own - e.g. based on client settings -
     * whether to show or hide the gui element.
     * <p>
     * If no elements are specified, this will reset all currently hidden elements
     *
     * @param element the {@link GuiElement} to reset
     */
    void resetElement(@NonNull GuiElement @Nullable ... element);

    /**
     * Determines whether a {@link GuiElement} is currently hidden.
     *
     * @param element the {@link GuiElement} to check
     */
    boolean isHudElementHidden(@NonNull GuiElement element);

    /**
     * Returns the currently hidden {@link GuiElement}s.
     *
     * @return an unmodifiable view of all currently hidden {@link GuiElement}s
     */
    @NonNull Set<GuiElement> hiddenElements();

    /**
     * Sends a notification toast to the client.
     */
    void sendToast(@NonNull String title, @NonNull String content);
}
