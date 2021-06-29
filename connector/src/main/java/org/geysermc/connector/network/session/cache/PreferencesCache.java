/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.CooldownUtils;

@Getter
public class PreferencesCache {
    private final GeyserSession session;

    /**
     * True if the client prefers being shown their coordinates, regardless if they're being shown or not.
     * This will be true everytime the client joins the server because neither the client nor server store the preference permanently.
     */
    @Setter
    private boolean prefersShowCoordinates = true;
    /**
     * If the client's preference will be ignored, this will return false.
     */
    private boolean allowShowCoordinates;

    /**
     * Which CooldownType the client prefers. Initially set to {@link CooldownUtils#getDefaultShowCooldown()}.
     */
    @Setter
    private CooldownUtils.CooldownType cooldownPreference = CooldownUtils.getDefaultShowCooldown();

    public PreferencesCache(GeyserSession session) {
        this.session = session;
    }

    /**
     * Tell the client to hide or show the coordinates.
     *
     * If {@link #prefersShowCoordinates} is true, coordinates will be shown, unless either of the following conditions apply: <br>
     * <br>
     * {@link GeyserSession#reducedDebugInfo} is enabled
     * {@link GeyserConfiguration#isShowCoordinates()} is disabled
     */
    public void updateShowCoordinates() {
        allowShowCoordinates = !session.isReducedDebugInfo() && session.getConnector().getConfig().isShowCoordinates();
        session.sendGameRule("showcoordinates", allowShowCoordinates && prefersShowCoordinates);
    }
}
