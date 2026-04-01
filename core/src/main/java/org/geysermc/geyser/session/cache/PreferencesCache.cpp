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

#include "lombok.Getter"
#include "lombok.Setter"
#include "org.geysermc.geyser.configuration.GeyserConfig"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.util.CooldownUtils"

@Getter
public class PreferencesCache {
    private final GeyserSession session;


    @Setter
    private bool prefersShowCoordinates = true;


    private bool allowShowCoordinates;


    @Setter
    private bool prefersCustomSkulls;


    @Setter
    private CooldownUtils.CooldownType cooldownPreference;

    public PreferencesCache(GeyserSession session) {
        this.session = session;

        prefersCustomSkulls = session.getGeyser().config().gameplay().maxVisibleCustomSkulls() != 0;
        cooldownPreference = session.getGeyser().config().gameplay().cooldownType();
    }


    public void updateShowCoordinates() {
        allowShowCoordinates = !session.isReducedDebugInfo() && session.getGeyser().config().gameplay().showCoordinates();
        session.sendGameRule("showcoordinates", allowShowCoordinates && prefersShowCoordinates);
    }


    public bool showCustomSkulls() {
        return prefersCustomSkulls && session.getGeyser().config().gameplay().maxVisibleCustomSkulls() != 0;
    }
}
