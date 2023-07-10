/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.preference;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.cumulus.component.Component;
import org.geysermc.cumulus.component.ToggleComponent;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.preference.BooleanPreference;
import org.geysermc.geyser.session.GeyserSession;

public class CustomSkullsPreference extends BooleanPreference {

    public static final String KEY = "geyser:show_custom_skulls";

    @Override
    public @NonNull Boolean defaultValue(GeyserConnection connection) {
        return isModifiable(connection);
    }

    @Override
    public boolean isModifiable(GeyserConnection connection) {
        GeyserSession session = (GeyserSession) connection;
        return session.getGeyser().getConfig().isAllowCustomSkulls();
    }

    @Override
    public Component component(@NonNull Boolean currentValue, GeyserConnection connection) {
        return ToggleComponent.of("geyser.settings.option.customSkulls", currentValue);
    }

    @Override
    public void onUpdate(@NonNull Boolean value, GeyserConnection connection) {
        //no-op
    }
}
