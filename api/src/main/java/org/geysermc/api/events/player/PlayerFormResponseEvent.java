/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.api.events.player;

import lombok.Getter;
import org.geysermc.api.Player;
import org.geysermc.api.window.FormWindow;
import org.geysermc.api.window.response.FormResponse;

/**
 * Called when a player interacts with a form
 */
public class PlayerFormResponseEvent extends PlayerEvent {

    @Getter
    private int formID;

    @Getter
    private FormWindow window;

    /**
     * Constructs a new PlayerFormResponseEvent instance
     *
     * @param player the player interacting with the form
     * @param formID the id of the form
     * @param window the window
     */
    public PlayerFormResponseEvent(Player player, int formID, FormWindow window) {
        super(player);

        this.formID = formID;
        this.window = window;
    }

    /**
     * Returns the response of the window, can be null
     * if the player closed the window
     *
     * @return the response of the window
     */
    public FormResponse getResponse() {
        return window.getResponse();
    }

    /**
     * Returns if the window is closed
     *
     * @return if the window is closed
     */
    public boolean isClosed() {
        return window.isClosed();
    }
}
