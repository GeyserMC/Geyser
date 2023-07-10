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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.preference.CooldownPreference;
import org.geysermc.geyser.preference.CustomSkullsPreference;
import org.geysermc.geyser.preference.PreferenceHolder;
import org.geysermc.geyser.preference.ShowCoordinatesPreference;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.CooldownUtils;

import java.util.Map;

public class PreferencesCache {
    private final GeyserSession session;

    @Getter
    private final Map<String, PreferenceHolder<?>> preferences = new Object2ObjectOpenHashMap<>();

    public PreferencesCache(GeyserSession session) {
        this.session = session;

        preferences.put(CooldownPreference.KEY, new PreferenceHolder<>(new CooldownPreference(), session));
        preferences.put(CustomSkullsPreference.KEY, new PreferenceHolder<>(new CustomSkullsPreference(), session));
        preferences.put(ShowCoordinatesPreference.KEY, new PreferenceHolder<>(new ShowCoordinatesPreference(), session));
    }

    @Nullable
    public <T> T getPreference(String key) {
        PreferenceHolder<T> holder = get(key);
        if (holder == null) {
            return null;
        }
        return holder.value();
    }

    public <T> T getPreference(String key, T defaultValue) {
        T value = getPreference(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private <T> PreferenceHolder<T> get(String key) {
        return (PreferenceHolder<T>) preferences.get(key);
    }

    /**
     * Tell the client to hide or show the coordinates. The client's preference will be overridden if either of the
     * following are true:
     * <br><br>
     * {@link GeyserSession#isReducedDebugInfo} is enabled.<br>
     * {@link GeyserConfiguration#isShowCoordinates()} is disabled.
     */
    public void updateShowCoordinates() {
        PreferenceHolder<Boolean> holder = get(ShowCoordinatesPreference.KEY);
        // preference itself won't be any different, but trigger an update anyway in case
        // reduced-debug-info has changed or the config has changed
        holder.preference().onUpdate(holder.value(), session);
    }

    /**
     * @return true if the session prefers custom skulls, and the config allows them.
     */
    public boolean showCustomSkulls() {
        if (session.getGeyser().getConfig().isAllowCustomSkulls()) {
            return false;
        }
        return getPreference(CustomSkullsPreference.KEY, true);
    }

    /**
     * @return the session's cooldown preference. does not take the config setting into account.
     */
    public CooldownUtils.CooldownType getCooldownPreference() {
        return getPreference(CooldownPreference.KEY, CooldownUtils.getDefaultShowCooldown());
    }
}
